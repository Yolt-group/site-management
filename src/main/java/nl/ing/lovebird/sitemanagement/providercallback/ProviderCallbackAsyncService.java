package nl.ing.lovebird.sitemanagement.providercallback;

import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.RefreshUserSitesFlywheelEvent;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatusValue;
import nl.ing.lovebird.providershared.callback.CallbackResponseDTO;
import nl.ing.lovebird.providershared.callback.UserDataCallbackResponse;
import nl.ing.lovebird.providershared.callback.UserSiteData;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.exception.CallbackIdentifierNotKnownException;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.UserSiteDataFetchInformation;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.providerresponse.ScrapingDataProviderResponseProcessor;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;

import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.SCR_CALLBACK;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProviderCallbackAsyncService {
    private final Clock clock;
    private final UserSiteService userSiteService;
    private final ScrapingDataProviderResponseProcessor scrapingDataProviderResponseProcessor;
    private final UserExternalIdRepository userExternalIdRepository;
    private final ActivityService activityService;
    private final SiteService siteService;
    private final FormProviderRestClient formProviderRestClient;
    private final ProviderRequestRepository providerRequestRepository;
    private final CallbackConfiguration callbackConfiguration;
    private final UserService userService;
    private final ClientTokenRequesterService callbacksClientTokenService;
    private final SiteManagementMetrics siteManagementMetrics;

    public void processCallback(CallbackResponseDTO callbackResponse)
            throws CallbackIdentifierNotKnownException {
        // When data was returned we should process it as before
        if (callbackResponse instanceof UserDataCallbackResponse userDataCallbackResponse) {
            // Budget Insight.
            handle(userDataCallbackResponse);
            return;
        }

        log.error("Unhandled callback class: {}", callbackResponse.getClass());
    }

    /**
     * @param subpath nullable subpath; used to distinguish callback types within a provider, such as SaltEdge
     */
    @Async(ApplicationConfiguration.ASYNC_EXECUTOR)
    void processCallbackDataAsync(String provider, @Nullable String subpath, String body) {

        if (siteService.getSites().stream().map(Site::getProvider).noneMatch(provider::equals)) {
            log.warn("Callback with non-recognized provider {} received", provider);
        }
        String externalUserId = JsonPath
                .parse(body)
                .read(callbackConfiguration.getUserIdJsonPathExpressions().get(provider), String.class);
        Optional<UserExternalId> externalIdUser = userExternalIdRepository.findByProviderAndExternalUserId(provider, externalUserId);
        if (externalIdUser.isEmpty()) {
            log.info("We got a callback from {} for user with externalId {}, which is unknown to us. We drop this callback", provider, externalUserId);
            siteManagementMetrics.scrapingProviderCallback(provider, "NOK_UNKNOWN_EXTERNAL_USER_ID");
            return;
        }
        final UUID userId = externalIdUser.get().getUserId();

        try (LogBaggage b = LogBaggage.builder().provider(provider).userId(userId).build()) {
            final Optional<User> findUser = userService.getUser(userId);
            if (findUser.isEmpty()) {
                log.info("We got a callback from {} for: [external_user_id={}, user_id={}].  Can't find the user.", provider, externalUserId, userId);
                siteManagementMetrics.scrapingProviderCallback(provider, "NOK_UNKNOWN_USER");
                return;
            }

            findUser.ifPresent(user -> {
                if (user.getStatus() == StatusType.BLOCKED) {
                    log.info("We got a callback from provider {} for a blocked user. Will not process data.", provider);
                    siteManagementMetrics.scrapingProviderCallback(provider, "NOK_BLOCKED_USER");
                    return;
                }

                List<PostgresUserSite> allUserSitesIncludingDeletedOnes =
                        userSiteService.getAllUserSitesIncludingDeletedOnes(userId).stream()
                                .filter(userSite -> userSite.getProvider().equals(provider))
                                .toList();
                if (allUserSitesIncludingDeletedOnes.isEmpty()) {
                    log.info("We got a callback from {}, but don't even have a usersite.", provider);
                    siteManagementMetrics.scrapingProviderCallback(provider, "NOK_UNKNOWN_USER_SITE");
                    return;
                }

                ClientUserToken clientUserToken = callbacksClientTokenService.getClientUserToken(user.getClientId().unwrap(), user.getUserId());

                Map<UUID, UUID> activeActivityPerUserSite = new HashMap<>();
                for (PostgresUserSite userSite : allUserSitesIncludingDeletedOnes) {
                    activeActivityPerUserSite.put(userSite.getUserSiteId(), userSiteService.checkLock(userSite)
                            .map(PostgresUserSiteLock::getActivityId)
                            .orElse(null));
                }

                CallbackRequestDTO callbackRequest = new CallbackRequestDTO(body, subpath, null, user.getClientId(),
                        getUserSiteDataFetchInformation(allUserSitesIncludingDeletedOnes), activeActivityPerUserSite);
                try {
                    formProviderRestClient.processCallback(provider, callbackRequest, clientUserToken);
                    siteManagementMetrics.scrapingProviderCallback(provider, "OK");
                } catch (HttpException e) {
                    siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_CALLBACK, provider, e);
                    throw new KnownProviderRestClientException(e);
                }
            });
        }
    }

    private List<UserSiteDataFetchInformation> getUserSiteDataFetchInformation(List<PostgresUserSite> allUserSitesIncludingDeletedOnes) {
        List<UserSiteDataFetchInformation> userSitesDataFetchInformation = new ArrayList<>();
        for (PostgresUserSite userSite : allUserSitesIncludingDeletedOnes) {
            UUID siteId = userSite.getSiteId();
            List<String> migratedAccountsExternalIds = Collections.emptyList();
            userSitesDataFetchInformation.add(
                    new UserSiteDataFetchInformation(
                            userSite.getExternalId(),
                            userSite.getUserSiteId(),
                            siteId, migratedAccountsExternalIds,
                            siteService.getSiteWhiteListedAccountType(siteId)));
        }
        return userSitesDataFetchInformation;
    }

    private void handle(UserDataCallbackResponse userDataCallbackResponse) throws CallbackIdentifierNotKnownException {
        String provider = userDataCallbackResponse.getProvider();
        String externalUserId = userDataCallbackResponse.getExternalUserId();

        UserSiteData userSiteData = userDataCallbackResponse.getUserSiteData();

        log.info("processing callback data of usersite with extId {}, with statuscode {}.",
                userSiteData.getExternalUserSiteId(),
                userSiteData.getProviderServiceResponseStatusValue().name()); //NOSHERIFF

        // Create DataProviderResponse
        // XXX Callbacks can also push data unsolicited, such as in the event of a SaltEdge /service callback or flywheel refresh
        ProviderServiceResponseStatusValue providerServiceResponseStatusValue = userSiteData.getProviderServiceResponseStatusValue();
        // Find matching userSite
        String externalUserSiteId = userSiteData.getExternalUserSiteId();
        PostgresUserSite userSite = getUserSite(provider, externalUserId, externalUserSiteId);

        try (LogBaggage b = new LogBaggage(userSite)) {
            // Process each normally
            process(providerServiceResponseStatusValue, userSite);
        }
    }

    private void process(ProviderServiceResponseStatusValue providerServiceResponseStatusValue, PostgresUserSite userSite) {
        UUID activityId;
        UserSiteActionType userSiteActionType;
        Optional<PostgresUserSiteLock> userSiteLock = userSiteService.checkLock(userSite);
        ClientUserToken clientUserToken = callbacksClientTokenService.getClientUserToken(userSite.getClientId().unwrap(), userSite.getUserId());

        // If the lock is present, we have an activity and providerRequest... That is, the activity that caused this callback data coming in.
        // If there is no lock, we assume it's a 'spontaneous' flywheel event triggered by the provider.
        if (userSiteLock.isPresent()) {
            activityId = userSiteLock.get().getActivityId();
            List<ProviderRequest> providerRequests = providerRequestRepository.find(userSite.getUserId(), activityId);
            if (providerRequests.isEmpty()) {
                log.error("Missing ProviderRequest for user-site {} and activity {}, its ttl has probably expired.  Sending activityEvent anyway.", userSite.getUserSiteId(), activityId);
                userSiteService.unlock(userSite);
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
                activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
                return;
            }
            // All the providerRequests that are done for 1 activity, should all have the same userActionType (i.e. CREATE_USER_SITE / USER_REFRESH / etc.)
            userSiteActionType = providerRequests.get(0).getUserSiteActionType();
        } else {
            activityId = UUID.randomUUID();
            userSiteActionType = UserSiteActionType.PROVIDER_FLYWHEEL_REFRESH;
            createAndPublishNewProviderFlywheelRefreshActivity(clientUserToken, userSite, activityId);
        }
        scrapingDataProviderResponseProcessor.process(userSite.getUserSiteId(), Optional.of(userSite), providerServiceResponseStatusValue, userSiteActionType, activityId, clientUserToken);
    }

    private void createAndPublishNewProviderFlywheelRefreshActivity(final @NonNull ClientUserToken clientUserToken,
                                                                    final PostgresUserSite userSite,
                                                                    final @NonNull UUID activityId) {
        final RefreshUserSitesFlywheelEvent refreshUserSitesEvent = new RefreshUserSitesFlywheelEvent(
                clientUserToken.getUserIdClaim(),
                activityId,
                ZonedDateTime.now(clock),
                Collections.singletonList(userSite.getUserSiteId())
        );
        activityService.startActivity(clientUserToken, refreshUserSitesEvent);
    }

    private PostgresUserSite getUserSite(String provider, String externalUserId, String externalUserSiteId)
            throws CallbackIdentifierNotKnownException {
        // The joys of having a database that doesn't support arbitrary queries
        UserExternalId userExternalId = userExternalIdRepository.findByProviderAndExternalUserId(provider, externalUserId)
                .orElseThrow(() -> new CallbackIdentifierNotKnownException("Could not process parsed callback data because we could not find user " + externalUserId + " for provider " + provider + "."));
        UUID userId = userExternalId.getUserId();
        List<PostgresUserSite> userSites = userSiteService.getAllUserSitesIncludingDeletedOnes(userId);
        return userSites.stream()
                .filter(userSiteInList -> externalUserSiteId.equals(userSiteInList.getExternalId()))
                .findAny().orElseThrow(() -> new CallbackIdentifierNotKnownException("Could not process parsed callback data because we could not find user-site  " + externalUserSiteId + " for provider " + provider + "."));
    }

    public void processNoSupportedAccountsFromCallback(final UUID userId, final UUID userSiteId) {
        PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
        Optional<PostgresUserSiteLock> userSiteLock = userSiteService.checkLock(userSite);
        ClientUserToken clientUserToken = callbacksClientTokenService.getClientUserToken(userSite.getClientId().unwrap(), userSite.getUserId());
        UUID activityId;
        UserSiteActionType userSiteActionType;

        // If there is no lock, we assume it's a 'spontaneous' flywheel event triggered by the provider.
        if (userSiteLock.isPresent()) {
            activityId = userSiteLock.get().getActivityId();
            List<ProviderRequest> providerRequests = providerRequestRepository.find(userId, activityId);

            if (providerRequests.isEmpty()) {
                log.error("Missing ProviderRequest for user-site {} and activity {}, its ttl has probably expired. No supported account found in callback data.", userSite.getUserSiteId(), activityId);
                userSiteActionType = UserSiteActionType.PROVIDER_CALLBACK;
            } else {
                // All the providerRequests that are done for 1 activity, should all have the same userActionType (i.e. CREATE_USER_SITE / USER_REFRESH / etc.)
                userSiteActionType = providerRequests.get(0).getUserSiteActionType();
            }
        } else {
            activityId = UUID.randomUUID();
            userSiteActionType = UserSiteActionType.PROVIDER_FLYWHEEL_REFRESH;
            createAndPublishNewProviderFlywheelRefreshActivity(clientUserToken, userSite, activityId);
        }
        scrapingDataProviderResponseProcessor.processNoSupportedAccountsMessage(userId, userSiteId, userSiteActionType);
    }
}
