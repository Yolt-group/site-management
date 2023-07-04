package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistUserSites;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.createRandomUserSite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
class UserSiteMaintenanceServiceIntegrationTest {

    @Autowired
    private Clock clock;

    @Autowired
    private UserSiteService userSiteService;

    @Autowired
    private PostgresUserSiteRepository userSiteRepository;

    @Autowired
    private UserSiteMaintenanceService userSiteMaintenanceService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserSiteEventService userSiteEventService;

    @BeforeEach
    public void cleanUp() {
        transactionTemplate.executeWithoutResult(transactionStatus -> entityManager.createNativeQuery("truncate table user_site cascade").executeUpdate());
        seedWithNonExpiredUserSiteStatusCodeNo();
    }

    @Test
    void shouldResetLastDataFetchForCorrectSite() {

        UUID SITE_ID1 = randomUUID();
        UUID SITE_ID2 = randomUUID();

        UUID userId = randomUUID();
        ClientId clientId = ClientId.random();

        final PostgresUserSite userSite1 = createRandomUserSite(clientId, SITE_ID1, userId);
        final PostgresUserSite userSite2 = createRandomUserSite(clientId, SITE_ID2, userId);

        userSiteRepository.save(userSite1);
        userSiteRepository.save(userSite2);

        userSiteMaintenanceService.resetLastDataFetchForSite(clientId, SITE_ID1);

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            PostgresUserSite updatedUserSite1 = userSiteService.getUserSite(userId, userSite1.getUserSiteId());
            assertThat(updatedUserSite1.getLastDataFetch()).isNull();
        });

        PostgresUserSite updatedUserSite2 = userSiteService.getUserSite(userId, userSite2.getUserSiteId());
        assertThat(updatedUserSite2.getLastDataFetch()).isNotNull();
    }


    @Test
    void shouldOnlyResetLastDataFetchForCorrectClient() {
        UUID siteId = randomUUID();
        UUID userId = randomUUID();
        ClientId clientIdA = ClientId.random();
        ClientId clientIdB = ClientId.random();

        final PostgresUserSite userSiteClientA = createRandomUserSite(clientIdA, siteId, userId);
        final PostgresUserSite userSiteClientB = createRandomUserSite(clientIdB, siteId, userId);

        userSiteRepository.save(userSiteClientA);
        userSiteRepository.save(userSiteClientB);

        userSiteMaintenanceService.resetLastDataFetchForSite(clientIdA, siteId);

        await().atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() -> assertThat(userSiteService
                        .getUserSite(userId, userSiteClientA.getUserSiteId()).getLastDataFetch()).isNull());

        PostgresUserSite userSiteSite2 = userSiteService.getUserSite(userId, userSiteClientB.getUserSiteId());
        assertThat(userSiteSite2.getLastDataFetch()).isNotNull();
    }

    private void seedWithNonExpiredUserSiteStatusCodeNo() {

        // not expired
        bulkPersistUserSites(ConnectionStatus.values().length, ClientId.random(), randomUUID(), UUID::randomUUID,
                (builder, i) -> builder
                        .connectionStatus(ConnectionStatus.values()[i])
                        .statusTimeoutTime(null)
                        .created(clock.instant().minus(60, ChronoUnit.MINUTES))
                        .updated(clock.instant().minus(1, ChronoUnit.MINUTES)), userSite -> {
                    System.out.println(userSite);
                    userSiteRepository.save(userSite);
                    return userSite;
                });
    }
}
