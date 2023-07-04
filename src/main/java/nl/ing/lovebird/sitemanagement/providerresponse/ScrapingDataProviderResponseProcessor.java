package nl.ing.lovebird.sitemanagement.providerresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.UpdateUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.ProviderServiceMAFResponseDTO;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.CustomExpiredConsentFlowService;
import nl.ing.lovebird.sitemanagement.exception.FailedToSerializeMfaFormException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsService;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.*;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.uuid.AISState;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ScrapingDataProviderResponseProcessor extends GenericDataProviderResponseProcessor {

    private final Clock clock;
    private final ConsentSessionService userSiteSessionService;
    private final ObjectMapper objectMapper;
    private final EncryptionDetailsService encryptionDetailsService;
    private final SitesProvider sitesProvider;

    public ScrapingDataProviderResponseProcessor(ActivityService activityService, //NOSONAR
                                                 UserSiteService userSiteService,
                                                 SiteManagementMetrics siteManagementMetrics,
                                                 CustomExpiredConsentFlowService customExpiredConsentFlowService,
                                                 ConsentSessionService userSiteSessionService,
                                                 ObjectMapper objectMapper,
                                                 EncryptionDetailsService encryptionDetailsService,
                                                 SitesProvider sitesProvider,
                                                 Clock clock) {
        super(activityService, userSiteService, siteManagementMetrics, customExpiredConsentFlowService);
        this.clock = clock;
        this.userSiteSessionService = userSiteSessionService;
        this.objectMapper = objectMapper;
        this.encryptionDetailsService = encryptionDetailsService;
        this.sitesProvider = sitesProvider;
    }

    /**
     * This method can be called when we got MFA from a scraper, when we also know what it was returned for.
     * The {@link ProviderRequest} contains information like what process we were doing that resulted in this MFA Form.
     * (i.e. 'create user site' , 'refresh user site' , '..')
     */
    public void processMfaWithKnownCause(
            final ProviderServiceMAFResponseDTO providerServiceMAFResponseDTO,
            final ProviderRequest providerRequest,
            final ClientUserToken clientUserToken
    ) {
        PostgresUserSite userSite = userSiteService.getUserSite(providerRequest.getUserId(), providerRequest.getUserSiteId());
        handleReceivedMfa(userSite, providerServiceMAFResponseDTO.getProviderMfaForm(), providerServiceMAFResponseDTO.getYoltMfaForm(), providerRequest.getActivityId(),
                providerRequest.getUserSiteActionType(), clientUserToken,
                providerServiceMAFResponseDTO.getMfaTimeout());
    }

    public void handleReceivedMfa(final PostgresUserSite userSite, String providerMfaForm, Form yoltMfaForm, UUID activityId, UserSiteActionType userSiteActionType, //NOSONAR
                                  ClientUserToken clientUserToken, Instant mfaTimeout) {
        siteManagementMetrics.incrementCounterMfaNeeded(userSiteActionType, userSite);
        EncryptionDetailsDTO encryptionDetails = encryptionDetailsService.getEncryptionDetails(userSite.getSiteId(), userSite.getProvider(), clientUserToken);
        Optional<ConsentSession> optionalConsentSession = userSiteSessionService.findByUserSiteId(userSite.getUserId(), userSite.getUserSiteId());
        Site site = sitesProvider.findByIdOrThrow(userSite.getSiteId());
        try (LogBaggage b = new LogBaggage(userSite)) {
            //
            // There are several possibilities:
            //
            // [1] !userSiteSession.isPresent()
            // [2]  userSiteSession.isPresent() && !activityId.equals(userSiteSession.get().getActivityId())
            // [3]  userSiteSession.isPresent() &&  activityId.equals(userSiteSession.get().getActivityId())

            if (optionalConsentSession.isEmpty()) {
                log.info("storeMfaForm [1] -- activityId == {}, userSiteSession == null.", activityId);
            } else {
                if (!activityId.equals(optionalConsentSession.get().getActivityId())) {
                    ConsentSession userSiteSession = optionalConsentSession.get();
                    // Case 2) this is the case that we now cover that should fix YCO-402
                    log.info("storeMfaForm [2] -- activityId ({} - {}) != userSiteSession.activityId.({} - {})", activityId, userSiteActionType,
                            userSiteSession.getActivityId(), userSiteSession.getOperation());
                    // Do *not* use the user site session, even though it exists.  It belongs to another activityId.
                    optionalConsentSession = Optional.empty();
                } else {
                    log.info("storeMfaForm [3] -- activityId == userSiteSession.activityId == {} - {}", activityId, userSiteActionType);
                }
            }

            // Update userSite.status
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.STEP_NEEDED, null, mfaTimeout);
            ConsentSession userSiteSession;
            if (optionalConsentSession.isPresent()) {
                userSiteSession = optionalConsentSession.get();
                String formStep = objectMapper.writeValueAsString(new FormStep(yoltMfaForm, null, encryptionDetails, providerMfaForm, userSiteSession.getStateId()));
                userSiteSessionService.updateWithNewStepAndProviderState(userSiteSession, null, formStep, null);
                // Because the scraping flow is async, we cannot know if the user successfully completed a step until we receive a callback from the scraper.
                // We should update the step number here so we handle the posted MFA form instead of defaulting to creating a new user-site
                // when a filled in MFA form is processed in CreateOrUpdateUserSiteService#processPostedLogin and CreateOrUpdateUserSiteService#isPostedLoginForNextStep
                userSiteSessionService.incrementCompletedSteps(userSiteSession);
            } else {
                // We can get here because of case 1 and 2: most likely because we got a MFA form during a refresh.
                // We must mark the user-site as 'failed' for the current running activity.  Do that now.
                activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.NEW_STEP_NEEDED);

                log.info("handleMFA -- creating a new userSiteSession and update activity.");
                var stateId = AISState.random();

                var newConsentSession = userSiteSessionService.createConsentSessionForRenewAccess(
                        stateId,
                        new ClientId(clientUserToken.getClientIdClaim()),
                        userSite,
                        new FormStep(yoltMfaForm, null, encryptionDetails, providerMfaForm, stateId.state()),
                        null,
                        null,
                        null
                );
                UUID newActivityId = newConsentSession.getActivityId();

                final UpdateUserSiteEvent updateUserSiteEvent = new UpdateUserSiteEvent(
                        userSite.getUserId(),
                        site.getId(),
                        newActivityId,
                        site.getName(),
                        ZonedDateTime.now(clock),
                        userSite.getUserSiteId());
                activityService.startActivity(clientUserToken, updateUserSiteEvent);
            }

            siteManagementMetrics.incrementCounterFetchDataFinish(userSiteActionType, userSite, RefreshedUserSiteEvent.Status.NEW_STEP_NEEDED);
            userSiteService.unlock(userSite);

        } catch (IOException e) {
            throw new FailedToSerializeMfaFormException(e);
        }
    }
}
