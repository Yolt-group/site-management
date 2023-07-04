package nl.ing.lovebird.sitemanagement.providerresponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatus;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatusValue;
import nl.ing.lovebird.providershared.form.ExtendedProviderServiceResponseStatus;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeans;
import nl.ing.lovebird.sitemanagement.accessmeans.CustomExpiredConsentFlowService;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.providershared.form.ExtendedProviderServiceResponseStatus.USERSITE_DOES_NOT_EXIST_ANYMORE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericDataProviderResponseProcessor {
    final ActivityService activityService;
    final UserSiteService userSiteService;
    final SiteManagementMetrics siteManagementMetrics;

    private final CustomExpiredConsentFlowService customExpiredConsentFlowService;

    /**
     * This method is used to process a ProviderResponse for NO_SUPPORTED_ACCOUNTS.
     * <p>
     * As of YCO-1216 we treat a {@link ProviderMessageType#NO_SUPPORTED_ACCOUNTS} message the same as a
     * successful refresh. The UserSite should be unlocked when we get the message but we should not update connection status.
     * Updating the UserSiteStatusCode should be done when we process the ingestion finished event.
     * That means we don't have to implement nasty hacks to notify our clients that a data fetch
     * failed and that the user-site is disconnected even though the {@link AccessMeans} for the user-site are still valid.
     */
    public void processNoSupportedAccountsMessage(final UUID userId,
                                                  final UUID userSiteId,
                                                  final UserSiteActionType userSiteActionType) {
        log.info("Data fetch resulted in no supported accounts for user-site with id {}.", userSiteId);
        PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
        siteManagementMetrics.incrementCounterFetchDataFinishSuccess(userSiteActionType, userSite);
        userSiteService.unlock(userSite);
    }

    public void process(final UUID userSiteId, //NOSONAR
                        final Optional<PostgresUserSite> optionalUserSite,
                        final ProviderServiceResponseStatusValue providerServiceResponseStatus,
                        final UserSiteActionType userSiteActionType,
                        final UUID activityId,
                        final @NonNull ClientUserToken clientUserToken) {
        // The (scraping) provider cannot find the user-site at their end ...
        if (providerServiceResponseStatus == USERSITE_DOES_NOT_EXIST_ANYMORE) {
            // ... so it must also be deleted at our end, validate this.
            if (optionalUserSite.isPresent() && !optionalUserSite.get().isDeleted()) {
                // This should --of course-- not occur.
                log.error("Received a message from providers for a user-site that is supposed to be deleted, but the user-site is not marked as deleted on our end. id = {}.", userSiteId);
            }
            // Send an activity event so that health knows that no data is coming in for this user-site.
            sendRefreshedUserSiteEventFailedForDeletedUserSite(clientUserToken, userSiteId, activityId);
            return;
        }

        // The user site has been deleted.
        if (optionalUserSite.isEmpty()) {
            log.error("Received a message from providers for a user-site that has been be deleted.");
            sendRefreshedUserSiteEventFailedForDeletedUserSite(clientUserToken, userSiteId, activityId);
            return;
        }

        // The user site has been marked for deletion.
        if (optionalUserSite.get().isDeleted()) {
            log.info("Received a message from providers for a user-site that is marked for deletion.");
            sendRefreshedUserSiteEventFailedForDeletedUserSite(clientUserToken, userSiteId, activityId);
            return;
        }

        // Now we're up to the point where we don't have the case where a user site might just be deleted.

        final PostgresUserSite userSite = optionalUserSite.get();
        try (LogBaggage b = new LogBaggage(userSite)) {

            /*
              You either do 1) or 2) in the huge switch case below, but not both:
               1) Set the userSiteStatusToSet and failureStatusForNotificationToHealth.
               2) handle the provider response. In that case, please be aware that you handle the following things appropriately.
               - user site action (used by DS)
               - activity event
               - user site status
               - user site lock
               - fetch data finish metric
             */
            final nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus connectionStatus;
            final nl.ing.lovebird.sitemanagement.usersite.FailureReason failureReason;
            final RefreshedUserSiteEvent.Status failureStatusForNotificationToHealth;

            if (providerServiceResponseStatus instanceof ProviderServiceResponseStatus) {
                switch ((ProviderServiceResponseStatus) providerServiceResponseStatus) {
                    // The bank is rate-limiting us.  This status can be returned *only* in case of flywheel refreshes.
                    case BACK_PRESSURE_REQUEST -> {
                        connectionStatus = ConnectionStatus.CONNECTED;
                        failureReason = null;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.OK_SUSPICIOUS;
                    }
                    case FINISHED -> {
                        // We explicitely don't handle the usersite status here. That will be set later when a 'aggregationFinished' event is
                        // is consumed. This event marks that the data is really persisted in our keyspaces.
                        siteManagementMetrics.incrementCounterFetchDataFinishSuccess(userSiteActionType, userSite);
                        userSiteService.unlock(userSite);
                        return;
                    }
                    case TOKEN_INVALID -> {
                        connectionStatus = customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite) ? ConnectionStatus.DISCONNECTED : ConnectionStatus.CONNECTED;
                        failureReason = FailureReason.CONSENT_EXPIRED;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.OK_SUSPICIOUS;
                    }
                    case UNKNOWN_ERROR -> {
                        connectionStatus = ConnectionStatus.CONNECTED;
                        failureReason = FailureReason.TECHNICAL_ERROR;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.FAILED;
                    }
                    default -> throw new IllegalStateException("Incomplete switch, missing case for ProviderServiceResponseStatus " + ((ProviderServiceResponseStatus) providerServiceResponseStatus).name());
                }
            } else if (providerServiceResponseStatus instanceof ExtendedProviderServiceResponseStatus) {
                // Scraping only:
                // Budget insight only.
                switch ((ExtendedProviderServiceResponseStatus) providerServiceResponseStatus) {
                    case LOGIN_SUCCEEDED -> {
                        userSiteService.updateUserSiteStatus(userSite, nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.CONNECTED, null, null);
                        return;
                    }
                    case FINISHED_WAITING_FOR_CALLBACK -> {
                        log.info("Not unlocking user-site {}, because we're waiting for a callback from {}. External user-site id: {}",
                                userSite.getUserSiteId(),
                                userSite.getProvider(),
                                userSite.getExternalId()); //NOSHERIFF
                        return;
                    }
                    case TOO_MANY_REFRESHES -> {
                        log.info("Too many refreshes. The data is already up to date. Setting status to REFRESH_FINISHED.");
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.OK_SUSPICIOUS;
                        connectionStatus = nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.CONNECTED;
                        failureReason = null;
                    }
                    case EXPIRED_CREDENTIALS, INCORRECT_ANSWER, INCORRECT_CREDENTIALS -> {
                        connectionStatus = nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.DISCONNECTED;
                        failureReason = nl.ing.lovebird.sitemanagement.usersite.FailureReason.AUTHENTICATION_FAILED;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.OK_SUSPICIOUS;
                    }
                    case SITE_ACTION_NEEDED -> {
                        connectionStatus = nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.CONNECTED;
                        failureReason = nl.ing.lovebird.sitemanagement.usersite.FailureReason.ACTION_NEEDED_AT_SITE;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.OK_SUSPICIOUS;
                    }
                    case SITE_ERROR -> {
                        connectionStatus = nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.CONNECTED;
                        failureReason = nl.ing.lovebird.sitemanagement.usersite.FailureReason.TECHNICAL_ERROR;
                        failureStatusForNotificationToHealth = RefreshedUserSiteEvent.Status.FAILED;
                    }
                    default -> throw new IllegalStateException("Incomplete switch, missing case for ExtendedProviderServiceResponseStatus " + ((ExtendedProviderServiceResponseStatus) providerServiceResponseStatus).name());
                }
            } else {
                // "Should never happen.." Just making the compiler happy.
                throw new IllegalStateException("Missing else-if branch for class " + providerServiceResponseStatus.getClass() +
                        " representing extension of enum : " + providerServiceResponseStatus.name());
            }


            log.info("Got response '{}' from providers for user-site {}, user-site-action-type {}, user-site-connection-status {}, user-site-failure-reason {}.",
                    providerServiceResponseStatus.name(),
                    userSite.getUserSiteId(),
                    userSiteActionType,
                    connectionStatus.name(),
                    failureReason != null ? failureReason.name() : null
            ); //NOSHERIFF

            userSiteService.updateUserSiteStatus(userSite,
                    connectionStatus,
                    failureReason,
                    null
            );
            activityService.handleFailedRefresh(clientUserToken, activityId, userSite, failureStatusForNotificationToHealth);
            siteManagementMetrics.incrementCounterFetchDataFinish(userSiteActionType, userSite, failureStatusForNotificationToHealth);
            userSiteService.unlock(userSite);
        }
    }

    void sendRefreshedUserSiteEventFailedForDeletedUserSite(
            @NonNull ClientUserToken clientUserToken,
            @NonNull UUID userSiteId,
            @NonNull UUID activityId
    ) {
        activityService.handleFailedRefresh(clientUserToken, activityId, userSiteId,
                ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, RefreshedUserSiteEvent.Status.FAILED);
    }
}
