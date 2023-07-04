package nl.ing.lovebird.sitemanagement.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationConstants;

import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisconnectUnusableUserSitesService {
    private final SiteService siteService;
    private final BatchUserSiteRepository batchUserSiteRepository;
    private final SiteManagementMetrics metrics;

    /**
     * Marks user-sites {@link ConnectionStatus#DISCONNECTED} when they are no longer usable. This is the case when the
     * related site is blacklisted, the related site is no longer supported, the user-site is in migration (we don't
     * expect those anymore) or the user-site has not been refreshed for at least 90 days. These are cases that are
     * skipped by the flywheel, with the last case to catch any unforseen cases (although that should not happen).
     *
     * @return the number of affected user-sites
     */
    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    public CompletableFuture<Long> disconnectAllUnusable(boolean dryrun) {

        log.info("Starting disconnection of all unusable user-sites.");

        final var allSites = siteService.getSites();

        final var affectedUnusableSite = allSites.stream()
                .filter(s -> false) // This was done based on 'noLongerSupported' property which is removed. If this batch needs to be reused, consider hardcoding siteId or via config properties.
                .flatMap(site -> Stream.of(batchUserSiteRepository.disconnectUserSitesForSite(site.getId(), dryrun)))
                .reduce(0L, Long::sum);

        final var affectedInMigration = batchUserSiteRepository.disconnectUserSitesWithMigrationStatus(MigrationConstants.IN_MIGRATION_STATUSES, dryrun);
        final var affectedNotRefreshedFor90Days = batchUserSiteRepository.disconnectUserSitesNotRefreshedFor90Days(dryrun);

        metrics.incrementDisconnectedUserSiteNotRefreshedFor90Days(affectedNotRefreshedFor90Days);

        log.info("Disconnected {} user-sites for blacklisted or no longer supported sites.", affectedUnusableSite);
        log.info("Disconnected {} user-sites that are in migration.", affectedInMigration);
        log.warn("Disconnected {} user-sites that were not refreshed for at least 90 days (this should never happen.", affectedNotRefreshedFor90Days);

        return completedFuture(affectedUnusableSite + affectedInMigration + affectedNotRefreshedFor90Days);
    }

}
