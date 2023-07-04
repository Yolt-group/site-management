package nl.ing.lovebird.sitemanagement.usersitedelete;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.DeleteUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.rest.deleteuser.UserDeleter;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.exception.UserSiteDeleteException;
import nl.ing.lovebird.sitemanagement.exception.UserSiteIsNotMarkedAsDeletedException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.sitemanagement.consentsession.GeneratedSessionStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

/**
 * This service is responsible for marking for delete, doing the internal deletes and calling another service for the external delete.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteDeleteService {
    private final Clock clock;
    private final PostgresUserSiteRepository postgresUserSiteRepository;
    private final MaintenanceClient maintenanceClient;
    private final ActivityService activityService;
    private final UserSiteDeleteProviderService userSiteDeleteProviderService;
    private final UserSiteEventService userSiteEventService;
    private final GeneratedSessionStateRepository generatedSessionStateRepository;
    private final ConsentSessionService userSiteSessionService;
    private final ClientTokenRequesterService clientTokenRequesterService;
    private final AccessMeansManager accessMeansManager;
    private final UserSiteService userSiteService;

    /**
     * This method is called from the user.
     * In this call the user-site gets deleted at the external provider, but it is marked for deletion internally.
     * We delete it at the external provider right away, since the scraping parties act differently and sometimes flaky after a user
     * immediately re-adds the bank after a deletion on Yolt. BI always returns the same instance of the user-site, Yodlee sometimes
     * returns the same one and sometimes creates a new one, SaltEdge fails the call.
     * This problem should be fixed when we immediately remove the user-site when the user asks us to.
     * It doesn't get deleted right away internally, since there might still be processes running asynchronously which need this data.
     */
    public void deleteExternallyAndMarkForInternalDeletion(
            final UUID userSiteId,
            final String psuIpAddress,
            final @NonNull ClientUserToken clientUserToken
    ) {
        log.info("Got request to mark user-site {} for deletion", userSiteId);

        PostgresUserSite userSite = postgresUserSiteRepository.getUserSite(clientUserToken.getUserIdClaim(), userSiteId)
                .orElse(null);
        if (userSite == null) {
            log.warn("The user site " + userSiteId + " does not exist (anymore?).");
            return;
        }

        if (userSite.isDeleted()) {
            log.warn("User site {} is already marked for deletion", userSiteId);
            return;
        }

        try (LogBaggage b = new LogBaggage(userSite)) {
            List<PostgresUserSite> otherUserSitesFromSameProvider = getOtherUserSitesFromSameProvider(userSite);

            markForInternalDeletionAndScheduleActualDeletion(userSite);
            userSiteDeleteProviderService.deleteUserSiteAsync(userSite, otherUserSitesFromSameProvider, psuIpAddress, clientUserToken);
            publishDeleteActivityEvent(clientUserToken, clientUserToken.getUserIdClaim(), userSiteId);
            userSiteEventService.publishDeleteUserSiteEvent(userSite, clientUserToken);
            userSiteSessionService.removeSessionsForUserSite(clientUserToken.getUserIdClaim(), userSiteId);
        }
    }

    /**
     * This method is called from maintenance.
     * Make sure this method is a blocking call, since maintenance will retry it once it fails.
     * This call will also try to remove this user-site at the provider in case it failed during the mark for internal deletion.
     */
    public void deleteUserSite(
            final UUID userId,
            final UUID userSiteId,
            final String psuIpAddress
    ) {
        log.info("Got request to delete user-site {} for user {}", userSiteId, userId);

        PostgresUserSite userSite = postgresUserSiteRepository.getUserSite(userId, userSiteId)
                .orElse(null);
        if (userSite == null) {
            log.info("Skipping delete, user site {} cannot be found, is it already deleted?", userSiteId);
            return;
        }

        if (!userSite.isDeleted()) {
            throw new UserSiteIsNotMarkedAsDeletedException(String.format("User site %s for user %s is not marked for delete", userSiteId, userId));
        }

        try (LogBaggage b = new LogBaggage(userSite)) {
            ClientUserToken clientUserToken = clientTokenRequesterService.getClientUserToken(userSite.getClientId().unwrap(), userSite.getUserId());

            List<PostgresUserSite> otherUserSitesFromSameProvider = getOtherUserSitesFromSameProvider(userSite);

            // External delete (best effort)
            try {
                userSiteDeleteProviderService.deleteUserSiteBlocking(userSite, otherUserSitesFromSameProvider, psuIpAddress, clientUserToken);
            } catch (Exception e) {
                log.error("Unable to delete user-site {} at provider {}. Ignoring.", userSite.getUserSiteId(), userSite.getProvider());
            }

            // Internal deletes
            userSiteService.unlock(userSite);

            final boolean userHasOtherUserSitesWithSameProvider;
            if (isScrapingSite(userSite.getProvider())) {
                userHasOtherUserSitesWithSameProvider = userSiteService.getAllUserSitesIncludingDeletedOnes(userSite.getUserId()).stream()
                        .filter(us -> !us.getUserSiteId().equals(userSite.getUserSiteId()))
                        .anyMatch(us -> us.getProvider().equals(userSite.getProvider()));
            } else {
                // Value doesn't matter.
                userHasOtherUserSitesWithSameProvider = false;
            }

            // External delete (best effort)
            try {
                accessMeansManager.deleteAccessMeansForUserSite(userSite, clientUserToken, userHasOtherUserSitesWithSameProvider);
            } catch (Exception e) {
                log.error("Unable to remove access means for user-site {} at provider {}. Ignoring.", userSite.getUserSiteId(), userSite.getProvider());
            }
            postgresUserSiteRepository.deleteUserSite(userSite.getUserSiteId());
        }
    }

    @Autowired
    void registerUserDeleter(final UserDeleter userDeleter) {
        userDeleter.registerDeleter(this::deleteUserCallFromMaintenance);
    }

    private List<PostgresUserSite> getOtherUserSitesFromSameProvider(PostgresUserSite userSite) {
        return postgresUserSiteRepository.getUserSites(userSite.getUserId())
                .stream()
                .filter(u -> !u.getUserSiteId().equals(userSite.getUserSiteId()))
                .filter(u -> u.getProvider().equals(userSite.getProvider()))
                .toList();
    }

    private void publishDeleteActivityEvent(final @NonNull ClientUserToken clientUserToken,
                                            final @NonNull UUID userId,
                                            final @NonNull UUID userSiteId) {
        UUID activityId = UUID.randomUUID();
        DeleteUserSiteEvent event = DeleteUserSiteEvent.builder()
                .activityId(activityId)
                .time(ZonedDateTime.now(clock))
                .userId(userId)
                .userSiteId(userSiteId)
                .build();
        activityService.startActivity(clientUserToken, event);
    }

    /**
     * You probably don't want to use this method, this skips the part that starts the datascience pipeline
     * <p>
     * Use {@link #deleteExternallyAndMarkForInternalDeletion}
     */
    public void scheduleUserSiteDeleteWithoutUserFeedback(UUID userId, UUID userSiteId, String externalUserSiteId) {
        userSiteService.markAsDeleted(userId, userSiteId);
        log.info("Marked user site {} with external id {} for deletion", userSiteId, externalUserSiteId); //NOSHERIFF
        maintenanceClient.scheduleUserSiteDelete(userId, userSiteId);
        log.info("Scheduled user site {} for deletion in maintenance", userSiteId);
    }

    /**
     * The actual delete query will be performed by Maintenance schedule
     */
    private void markForInternalDeletionAndScheduleActualDeletion(final PostgresUserSite userSite) {
        final UUID userId = userSite.getUserId();
        final UUID userSiteId = userSite.getUserSiteId();
        final String externalUserSiteId = userSite.getExternalId();

        try {
            userSiteService.markAsDeleted(userId, userSiteId);
            log.info("Marked user site {} with external id {} for deletion", userSiteId, externalUserSiteId); //NOSHERIFF

            maintenanceClient.scheduleUserSiteDelete(userId, userSiteId);
            log.info("Scheduled user site {} for deletion in maintenance", userSiteId);
        } catch (Exception e) {
            // either C* call or REST call may fail with exception
            log.error("Failed to mark user site {} of user {} with external id {} for internal deletion", userSiteId, userId, externalUserSiteId, e);
            throw new UserSiteDeleteException(String.format("Failed to mark user site %s of user %s with external id %s for internal deletion",
                    userSiteId, userId, externalUserSiteId), e);
        }
    }

    /**
     * Delete user-related information and only mark all user-sites for deletion, maintenance will pick up the schedule
     * and other actions will be performed when the last user-site is deleted
     *
     * @param clientUserToken - client user token for the user to be removed
     */
    private void deleteUserCallFromMaintenance(final ClientUserToken clientUserToken) {
        UUID userId = clientUserToken.getUserIdClaim();
        postgresUserSiteRepository.getUserSites(userId).forEach(userSite -> {
            try (LogBaggage b = new LogBaggage(userSite)) {
                markForInternalDeletionAndScheduleActualDeletion(userSite);
            }
        });

        generatedSessionStateRepository.delete(userId);
    }

}
