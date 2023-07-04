package nl.ing.lovebird.sitemanagement.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * This class is used to schedule {@link PostgresUserSite}s for deletion in a batch job.
 * We cannot remove a user-site from our database when we delete them because we store data related to the user-site
 * in other services.
 * By scheduling a deletion through the {@link MaintenanceClient} we can ensure the deletion is propagated to all related
 * services.
 * As part of the cleanup process in the maintenance service, we also clean up the data in this service's database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class BatchUserSiteDeleteService {

    private final PostgresUserSiteRepository userSiteRepository;
    private final UserSiteDeleteService userSiteDeleteService;

    /**
     * Schedules user-sites for deletion based on the site-id of the user-site.
     * A deletion is not directly executed on our database but rather, it is scheduled through our maintenance pod.
     * This means that the actual deletion will happen some time after this method is finished.
     *
     * @param siteId               the site-id to delete the user-sites for
     * @param maxUserSitesToDelete the maximum number of user-sites to delete
     * @param dryrun               if set to false, no user-sites will be scheduled for deletion
     * @return the number of user-sites that were deleted
     */
    int scheduleUserSitesForDeletionForSite(UUID siteId, int maxUserSitesToDelete, boolean dryrun) {
        var userSitesToDelete = userSiteRepository.getUserSitesBySite(siteId, maxUserSitesToDelete);

        if (dryrun) {
            log.info("delete user-sites for site would schedule {} user-sites for deletion for site {}", userSitesToDelete.size(), siteId);
            userSitesToDelete.forEach(userSite -> {
                try (LogBaggage b = new LogBaggage(userSite)) {
                    log.info("delete user-sites for site would schedule user site {} for deletion.", userSite.getUserSiteId());
                }
            });
        } else {
            log.info("scheduling {} user-sites for deletion for site {}", userSitesToDelete.size(), siteId);
            userSitesToDelete.forEach(userSite -> {
                try (LogBaggage b = new LogBaggage(userSite)) {
                    scheduleForDeletion(userSite);
                }
            });
        }
        return userSitesToDelete.size();
    }


    /**
     * Explicitly *do not* delete the UserSite from the database. This call publishes a delete event
     * the maintenance pod will pick that up and perform the actual deletion on :00 of every hour.
     *
     * @param postgresUserSite the user-site to schedule for deletion
     */
    private void scheduleForDeletion(PostgresUserSite postgresUserSite) {
        userSiteDeleteService.scheduleUserSiteDeleteWithoutUserFeedback(
                postgresUserSite.getUserId(),
                postgresUserSite.getUserSiteId(),
                postgresUserSite.getExternalId()
        );
    }
}
