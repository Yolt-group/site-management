package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteMaintenanceRepository.SimpleUserSiteId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistUserSites;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
class ResetLastDataFetchTest {

    @Autowired
    private PostgresUserSiteRepository userSiteRepository;

    @Autowired
    private UserSiteMaintenanceService userSiteMaintenanceService;

    @Autowired
    private Clock clock;

    @Test
    void shouldResetLastDataFetchForSite() {

        // Add non-matching user-sites (random seed)
        bulkPersistUserSites(10, ClientId.random(), randomUUID(), UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(Instant.now(clock)), userSite -> {
                    userSiteRepository.save(userSite);
                    return userSite;
                });

        // Add matching user-sites
        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        List<PostgresUserSite> userSites = bulkPersistUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(Instant.now(clock)), userSite -> {
                    userSiteRepository.save(userSite);
                    return userSite;
                });

        // assert only 31 are changed
        List<SimpleUserSiteId> affected = userSiteMaintenanceService.resetLastDataFetchForSite(clientId, siteId).join();
        assertThat(affected).hasSize(31);
    }
}
