package nl.ing.lovebird.sitemanagement.usersite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteMaintenanceRepository.SimpleUserSiteId;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteMaintenanceService {
    private final PostgresUserSiteMaintenanceRepository userSiteMaintenanceRepository;

    /**
     * Asynchronously reset the last data fetch property on {@link PostgresUserSite}s for the given <code>clientId</code> and <code>siteId</code>.
     * <p/>
     * This method resets the following:
     * <ul>
     * <li>last data fetch on the user-site</li>
     * <li>last transaction id for every account in the user-site</li>
     * </ul>
     * <p/>
     * This operation only applies to {@link PostgresUserSite}s created and/or updated in the last 180 days.
     *
     * @param clientId the client-id identifying the client
     * @param siteId   the site-id identifying the {@link Site}
     * @return the number of user-sites affected
     */
    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    public CompletableFuture<List<SimpleUserSiteId>> resetLastDataFetchForSite(final ClientId clientId, final UUID siteId) {

        List<SimpleUserSiteId> affected = resetLastDataFetchForSiteLast180Days(clientId, siteId);
        log.info("Reset last data fetch property on {} user-sites for client/{} and site/{}", affected.size(), clientId.unwrap(), siteId);

        return completedFuture(affected);
    }

    /**
     * Reset the last data fetch property {@link PostgresUserSite}s for the given <code>clientId</code> and <code>siteId</code>
     * for {@link PostgresUserSite}s created or updates in the last 180 days.
     *
     * @param clientId the client-id identifying the client
     * @param siteId   the site-id identifying the {@link Site}
     * @return the number of user-sites affected
     */
    private List<SimpleUserSiteId> resetLastDataFetchForSiteLast180Days(final ClientId clientId, final UUID siteId) {
        return userSiteMaintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, siteId);
    }
}
