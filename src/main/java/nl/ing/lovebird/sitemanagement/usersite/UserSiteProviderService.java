package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.CreateUserSiteEvent;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.UpdateUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.providershared.form.FormSiteLoginFormDTO;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansHolder;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.*;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.singleton;
import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.*;
import static nl.ing.lovebird.sitemanagement.usersite.ProcessedStepResult.*;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteProviderService {

    private final Clock clock;
    private final LoginFormService loginFormService;
    private final ProviderRequestRepository providerRequestRepository;
    private final SiteManagementMetrics siteManagementMetrics;
    private final ConsentSessionService userSiteSessionService;
    private final ObjectMapper objectMapper;
    private final UserSiteService userSiteService;
    private final ProviderRestClient providerRestClient;
    private final FormProviderRestClient formProviderRestClient;
    private final ActivityService activityService;
    private final AccessMeansManager accessMeansManager;
    private final UserSiteRefreshService userSiteRefreshService;
    private final AuthenticationMeansFactory authenticationMeansFactory;
    private final UserSitePersistedFormAnswersService userSitePersistedFormAnswersService;
    private final LastFetchedService lastFetchedService;
    private final ClientSiteService clientSiteService;

    public ProcessedStepResult updateUserSiteForScrapingProvider(
            final FormLogin login,
            final PostgresUserSite userSite,
            final Site site,
            final UUID activityId,
            final ClientUserToken clientUserToken
    ) {
        String provider = userSite.getProvider();
        if (!isScrapingSite(provider)) {
            throw new UnsupportedOperationException("Updating an external user site can only be performed for scraping (form) providers.");
        }

        AccessMeansManager.AccessMeansResult accessMeansResult = accessMeansManager.retrieveValidAccessMeans(
                clientUserToken,
                userSite,
                Instant.now(clock),
                null // not relevant for scraping providers
        );
        switch (accessMeansResult.getResultCode()) {
            case ACCESS_MEANS_DO_NOT_EXIST:
                userSiteService.unlock(userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                log.error("Error while updating a user-site for a scraping provider: no access means in database.");
                return loginFailed(userSite.getUserSiteId());
            case UNKNOWN_ERROR:
                userSiteService.unlock(userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
                log.error("Error while updating a user-site for a scraping provider.");
                return loginFailed(userSite.getUserSiteId());
            case OK:
                break;
            default:
                throw new IllegalStateException();
        }

        final Instant startDate = lastFetchedService
                .determineTransactionRetrievalLowerBoundTimestamp(new ClientId(clientUserToken.getClientIdClaim()),
                        Optional.ofNullable(userSite.getLastDataFetch()), Optional.empty());

        try {
            SiteLoginForm loginForm = loginFormService.getLoginForm(site, clientUserToken);

            FormSiteLoginFormDTO formSiteLoginForm = loginForm == null ? null : new FormSiteLoginFormDTO(
                    loginForm.getLoginFormJson(),
                    loginForm.getAltLoginFormJson()
            );
            FormUserSiteDTO formUserSite = new FormUserSiteDTO(
                    userSite.getUserId(),
                    userSite.getUserSiteId(),
                    userSite.getExternalId(),
                    userSite.getLastDataFetch() != null ? Date.from(userSite.getLastDataFetch()) : null
            );

            ProviderRequest providerRequest = new ProviderRequest(
                    UUID.randomUUID(),
                    activityId,
                    userSite.getUserId(),
                    userSite.getUserSiteId(),
                    UserSiteActionType.UPDATE_USER_SITE
            );
            providerRequestRepository.saveValidated(providerRequest);
            List<String> migratedAccountExternalIds = Collections.emptyList();

            FormUpdateExternalUserSiteDTO formUpdateExternalUserSite = new FormUpdateExternalUserSiteDTO(
                    site.getExternalId(),
                    formUserSite,
                    accessMeansResult.getAccessMeans().toAccessMeansDTO().getAccessMeansBlob(),
                    startDate,
                    login.getFilledInUserSiteFormValues(),
                    formSiteLoginForm,
                    userSite.getClientId(),
                    providerRequest.getId(),
                    new UserSiteDataFetchInformation(
                            userSite.getExternalId(),
                            userSite.getUserSiteId(),
                            userSite.getSiteId(),
                            migratedAccountExternalIds,
                            site.getAccountTypeWhitelist()
                    ),
                    site.getId(),
                    activityId
            );

            activityService.startActivity(clientUserToken, new UpdateUserSiteEvent(
                    clientUserToken.getUserIdClaim(),
                    userSite.getUserSiteId(),
                    activityId,
                    site.getName(),
                    ZonedDateTime.now(clock),
                    userSite.getUserSiteId()
            ));
            formProviderRestClient.updateExternalUserSite(provider, formUpdateExternalUserSite, clientUserToken);
            return activity(userSite.getUserSiteId(), activityId);

        } catch (HttpException e) {
            log.error("Failed to send updated credentials to providers for a scraping provider.", e);
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SiteManagementMetrics.ProvidersFunction.SCR_UPDATE_USER_SITE, provider, e);
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            return loginFailed(userSite.getUserSiteId());
        } catch (Exception e) {
            log.error("Failed to send updated credentials to providers for a scraping provider " + userSite.getProvider(), e);
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            return loginFailed(userSite.getUserSiteId());
        }
    }

    public ProcessedStepResult submitMfa(
            final Site site,
            final FormLogin loginMfa,
            final PostgresUserSite userSite,
            final ConsentSession userSiteSession,
            final ClientUserToken clientUserToken
    ) {
        if (!isScrapingSite(userSite.getProvider())) {
            throw new UnsupportedOperationException("Submitting MFA is only available for scraping providers");
        }

        final UUID activityId = userSiteSession.getActivityId();

        AccessMeansManager.AccessMeansResult accessMeansResult = accessMeansManager.retrieveValidAccessMeans(
                clientUserToken,
                userSite,
                Instant.now(clock),
                null
        );

        try {
            switch (accessMeansResult.getResultCode()) {
                case UNKNOWN_ERROR:
                    userSiteService.unlock(userSite);
                    userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
                    log.error("Error while processing MFA form for a scraping provider: unknown error.");
                    return loginFailed(userSite.getUserSiteId());
                case ACCESS_MEANS_DO_NOT_EXIST:
                    userSiteService.unlock(userSite);
                    userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                    log.error("Error while processing MFA form for a scraping provider: no access means in db.");
                    return loginFailed(userSite.getUserSiteId());
                case OK:
                    break;
                default:
                    throw new IllegalStateException();
            }

            final Instant startDate = lastFetchedService
                    .determineTransactionRetrievalLowerBoundTimestamp(new ClientId(clientUserToken.getClientIdClaim()),
                            Optional.ofNullable(userSite.getLastDataFetch()), Optional.empty());

            FormStep formStep = objectMapper.readValue(userSiteSession.getFormStep(), FormStep.class);

            FormUserSiteDTO formUserSite = new FormUserSiteDTO(userSite.getUserId(), userSite.getUserSiteId(),
                    userSite.getExternalId(), userSite.getLastDataFetch() != null ? Date.from(userSite.getLastDataFetch()) : null);

            ProviderRequest providerRequest = new ProviderRequest(UUID.randomUUID(), activityId, userSite.getUserId(), userSite.getUserSiteId(),
                    userSiteSession.getOperation().toUserActionType());
            providerRequestRepository.saveValidated(providerRequest);
            List<String> migratedAccountExternalIds = Collections.emptyList();

            FormSubmitMfaDTO formSubmitMfaDTO = new FormSubmitMfaDTO(
                    site.getExternalId(),
                    formUserSite,
                    accessMeansResult.getAccessMeans().toAccessMeansDTO().getAccessMeansBlob(),
                    startDate,
                    formStep.getSerializedProviderForm(),
                    loginMfa.getFilledInUserSiteFormValues(),
                    userSite.getClientId(),
                    providerRequest.getId(),
                    new UserSiteDataFetchInformation(
                            userSite.getExternalId(),
                            userSite.getUserSiteId(),
                            userSite.getSiteId(),
                            migratedAccountExternalIds,
                            site.getAccountTypeWhitelist()
                    ),
                    activityId,
                    site.getId()
            );
            formProviderRestClient.submitMfa(userSite.getProvider(), formSubmitMfaDTO, clientUserToken);
            return activity(userSite.getUserSiteId(), activityId);
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_UPDATE_USER_SITE, userSite.getProvider(), e);
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            log.error("Http exception while submitting mfa to providers.", e);
            return loginFailed(userSite.getUserSiteId());
        } catch (Exception e) {
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            log.error("Unknown exception while submitting mfa to provider " + userSite.getProvider(), e);
            return loginFailed(userSite.getUserSiteId());
        }
    }

    @SuppressWarnings("squid:S00107")
    public ProcessedStepResult createUserSiteForScrapingProvider(
            Login login,
            Site site,
            PostgresUserSite userSite,
            UUID activityId,
            ClientUserToken clientUserToken
    ) {
        final AccessMeansHolder accessMeans;
        AccessMeansManager.AccessMeansResult accessMeansResult = accessMeansManager.retrieveValidAccessMeans(clientUserToken, userSite, Instant.now(clock), null);
        switch (accessMeansResult.getResultCode()) {
            case ACCESS_MEANS_DO_NOT_EXIST:
                // We don't have accessMeans yet, so we'll need to make a new user at the scraping provider.
                try {
                    accessMeans = accessMeansManager.createUserForScrapingProvider(userSite, clientUserToken);
                } catch (RuntimeException e) {
                    userSiteService.unlock(userSite);
                    userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                    log.error("Error while creating a user at a scraping provider.", e);
                    return loginFailed(userSite.getUserSiteId());
                }
                break;
            case UNKNOWN_ERROR:
                userSiteService.unlock(userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
                log.error("Error while creating a user-site for a scraping provider.  We do have accessMeans in the database, but couldn't renew them.");
                return loginFailed(userSite.getUserSiteId());
            case OK:
                accessMeans = accessMeansResult.getAccessMeans();
                break;
            default:
                throw new IllegalStateException();
        }

        try {
            final Instant startDate = lastFetchedService
                    .determineTransactionRetrievalLowerBoundTimestamp(new ClientId(clientUserToken.getClientIdClaim()),
                            Optional.ofNullable(userSite.getLastDataFetch()), Optional.empty());
            SiteLoginForm siteLoginForm = loginFormService.getLoginForm(site, clientUserToken);

            ProviderRequest providerRequest = new ProviderRequest(UUID.randomUUID(), activityId, userSite.getUserId(), userSite.getUserSiteId(), UserSiteActionType.CREATE_USER_SITE);
            providerRequestRepository.saveValidated(providerRequest);

            FormSiteLoginFormDTO formSiteLoginFormDTO = null;
            if (siteLoginForm != null) {
                formSiteLoginFormDTO = new FormSiteLoginFormDTO(siteLoginForm.getLoginFormJson(), siteLoginForm.getAltLoginFormJson());
            }
            List<String> migratedAccountExternalIds = Collections.emptyList();

            FormCreateNewExternalUserSiteDTO formCreateNewExternalUserSiteDTO = new FormCreateNewExternalUserSiteDTO(
                    site.getExternalId(),
                    userSite.getUserId(),
                    userSite.getUserSiteId(),
                    accessMeans.toAccessMeansDTO(),
                    startDate,
                    ((FormLogin) login).getFilledInUserSiteFormValues(),
                    formSiteLoginFormDTO,
                    new ClientId(clientUserToken.getClientIdClaim()),
                    providerRequest.getId(),
                    new UserSiteDataFetchInformation(
                            userSite.getExternalId(),
                            userSite.getUserSiteId(),
                            userSite.getSiteId(),
                            migratedAccountExternalIds,
                            site.getAccountTypeWhitelist()
                    ),
                    activityId,
                    site.getId()
            );
            activityService.startActivity(clientUserToken, new CreateUserSiteEvent(
                    clientUserToken.getUserIdClaim(),
                    userSite.getSiteId(),
                    activityId,
                    site.getName(),
                    ZonedDateTime.now(clock),
                    userSite.getUserSiteId()
            ));
            formProviderRestClient.createNewExternalUserSite(userSite.getProvider(), formCreateNewExternalUserSiteDTO, clientUserToken);
            // This counter is in the right place.  A data fetch is implicit for scraping providers, it's not a separate operation.
            siteManagementMetrics.incrementCounterFetchDataStart(UserSiteActionType.CREATE_USER_SITE, userSite);

            return activity(userSite.getUserSiteId(), activityId);
        } catch (HttpException e) {
            log.error("Failed to create an external user-site at a scraping provider for user-site " + userSite.getUserSiteId() + " and provider " + userSite.getProvider() + '.', e);
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_CREATE_USER_SITE, userSite.getProvider(), e);
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            return loginFailed(userSite.getUserSiteId());
        } catch (RuntimeException e) {
            log.error("Failed to create an external user-site at a scraping provider for user-site " + userSite.getUserSiteId() + " and provider " + userSite.getProvider() + '.', e);
            userSiteService.unlock(userSite);
            userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
            return loginFailed(userSite.getUserSiteId());
        }
    }

    @SuppressWarnings("squid:S00107")
    public ProcessedStepResult processStepForDirectConnectionProvider(
            @NonNull Login login,
            PostgresUserSite userSite,
            @NonNull ConsentSession userSiteSession,
            @Nullable String psuIpAddress,
            @NonNull ClientUserToken clientUserToken,
            @NonNull UserSiteActionType userSiteActionType,
            @NonNull String baseClientRedirectUrl
    ) {
        try (LogBaggage ignored = new LogBaggage(userSite)) {
            final AccessMeansDTO accessMeansDTO;
            try {
                final AuthenticationMeansReference authenticationMeansReference = authenticationMeansFactory.createAuthMeans(clientUserToken, userSite.getRedirectUrlId());
                final ApiCreateAccessMeansDTO apiCreateAccessMeansDTO;

                ClientId clientId = new ClientId(clientUserToken.getClientIdClaim());
                UUID siteId = userSite.getSiteId();
                boolean forceExperimentalVersion = clientSiteService.isClientUsingExperimentalVersion(clientId, siteId);
                if (login instanceof UrlLogin urlLogin) {
                    final String redirectUrlPostedBackFromSite;
                    if (!clientUserToken.isPSD2Licensed()) {
                        redirectUrlPostedBackFromSite = ClientRedirectUrlService.changeRedirectBaseUrlOfUrlPostedBackFromSite(baseClientRedirectUrl, urlLogin.getRedirectUrl());
                    } else {
                        redirectUrlPostedBackFromSite = urlLogin.getRedirectUrl();
                    }

                    // Validate that redirectUrlPostedBackFromSite is a valid OAuth2 redirection URI, if it is not
                    // there is no use in sending it to providers and we can mark the site as login failed immediately.
                    if (!OAuth2RedirectionURI.parse(redirectUrlPostedBackFromSite).isValid()) {
                        log.warn("Not sending URI \"{}\" to providers because it is invalid, marking site as LOGIN_FAILED directly.", redirectUrlPostedBackFromSite);
                        userSiteService.unlock(userSite);
                        userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                        return loginFailed(userSite.getUserSiteId());
                    }

                    log.info("Sending valid redirect URI \"{}\" posted back from site to providers", redirectUrlPostedBackFromSite);

                    apiCreateAccessMeansDTO = new ApiCreateAccessMeansDTO(
                            login.getUserId(),
                            authenticationMeansReference,
                            userSiteSession.getProviderState(),
                            null,
                            psuIpAddress,
                            userSiteSession.getStateId(),
                            redirectUrlPostedBackFromSite,
                            baseClientRedirectUrl
                    );
                } else {
                    apiCreateAccessMeansDTO = new ApiCreateAccessMeansDTO(
                            login.getUserId(),
                            authenticationMeansReference,
                            userSiteSession.getProviderState(),
                            ((FormLogin) login).getFilledInUserSiteFormValues(),
                            psuIpAddress,
                            userSiteSession.getStateId(),
                            null,
                            baseClientRedirectUrl
                    );
                }

                final AccessMeansOrStepDTO accessMeansOrStep;
                try {
                    accessMeansOrStep = providerRestClient.createNewAccessMeans(
                            userSite.getProvider(),
                            userSite.getSiteId(),
                            apiCreateAccessMeansDTO,
                            clientUserToken,
                            forceExperimentalVersion
                    );
                    // We want to always send the stateId to our clients to allow them to easily relate the state
                    // in the redirectUrl/form to a user or activity in their own system.
                    var step = accessMeansOrStep.getStep();
                    if (step != null) {
                        step.setStateId(userSiteSession.getStateId());
                    }
                } catch (HttpException e) {
                    siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(CREATE_ACCESS_MEANS, userSite.getProvider(), e);
                    log.error("Failed to create access means.", e);
                    userSiteService.unlock(userSite);
                    userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                    return loginFailed(userSite.getUserSiteId());
                }

                // Remember values that the user provided if so required.
                if (login instanceof FormLogin) {
                    Map<String, String> filledInFormValues = ((FormLogin) login).getFilledInUserSiteFormValues().getValueMap();
                    userSitePersistedFormAnswersService.persistFormFieldAnswers(userSiteSession.getFormStep(), userSite, filledInFormValues);
                }

                if (accessMeansOrStep.getStep() != null) {
                    try {
                        saveNewStep(accessMeansOrStep.getStep(), userSiteSession, userSite);
                    } catch (JsonProcessingException e) {
                        userSiteService.unlock(userSite);
                        userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                        log.error("Failed to save step for " + userSite.getProvider(), e);
                        return loginFailed(userSite.getUserSiteId());
                    }
                    return step(userSite.getUserSiteId(), accessMeansOrStep.getStep());
                }

                // We have accessMeans.
                accessMeansDTO = accessMeansOrStep.getAccessMeans();
                accessMeansManager.upsertUserSiteAccessMeans(accessMeansDTO, userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, null, null);
            } catch (KnownProviderRestClientException e) {
                userSiteService.unlock(userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);
                log.error("Failed to process step for a direct connection provider " + userSite.getProvider(), e);
                return loginFailed(userSite.getUserSiteId());
            }

            // Make a blocking call so we can determine if the calls to providers and health are successful (i.e. has the activity been started).
            return userSiteRefreshService.refreshUserSitesBlocking(
                            singleton(userSite),
                            clientUserToken,
                            userSiteActionType,
                            psuIpAddress,
                            userSiteSession.getActivityId())
                    .map(activityId -> activity(userSite.getUserSiteId(), activityId))
                    .orElseGet(() -> markActivityAsFailedAndReturn(userSite));
        }
    }

    private void saveNewStep(Step step, ConsentSession userSiteSession, PostgresUserSite userSite) throws JsonProcessingException {
        if (step instanceof FormStep stepFromProviders) {
            // The form, like a redirect-url,  should contain the stateId. The stateId should be submitted back by the client,
            // so we can relate the submitted form the the session. In other words, so we know what we were doing.
            stepFromProviders.setStateId(userSiteSession.getStateId());
            String serializedFormStep = objectMapper.writeValueAsString(stepFromProviders);
            userSiteSessionService.updateWithNewStepAndProviderState(userSiteSession, step.getProviderState(), serializedFormStep, null);
        } else if (step instanceof RedirectStep) {
            String serializedRedirectStep = objectMapper.writeValueAsString(step);
            userSiteSessionService.updateWithNewStepAndProviderState(userSiteSession, step.getProviderState(), null, serializedRedirectStep);
        } else {
            throw new UnsupportedOperationException("No implementation for step " + step.getClass());
        }

        // Step has been completed successfully if no exception was thrown during the call to providers.
        // Make a note of this in the ConsentSession.
        userSiteSessionService.incrementCompletedSteps(userSiteSession);

        userSiteService.unlock(userSite);
        userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.STEP_NEEDED, null, null);
    }

    /**
     * If the activity was not started or if the call to providers failed, we should not return the activity id and update the usersite status
     * to reflect this.
     *
     * @param userSite the usersite that failed to refresh
     * @return {@link ProcessedStepResult} with no activityId
     */
    private ProcessedStepResult markActivityAsFailedAndReturn(PostgresUserSite userSite) {

        userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
        return noActivity(userSite.getUserSiteId());
    }

}
