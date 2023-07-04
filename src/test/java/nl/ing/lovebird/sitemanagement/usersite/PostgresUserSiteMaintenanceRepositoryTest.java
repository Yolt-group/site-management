package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import nl.ing.lovebird.sitemanagement.lib.MutableClock;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteMaintenanceRepository.SimpleUserSiteId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistPostgresUserSites;
import static org.assertj.core.api.Assertions.assertThat;

@TestContainerDataJpaTest
class PostgresUserSiteMaintenanceRepositoryTest {

    @Autowired
    PostgresUserSiteRepository repository;

    @Autowired
    PostgresUserSiteMaintenanceRepository maintenanceRepository;

    @Autowired
    MutableClock clock;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.asFixed(LocalDateTime.parse("2016-08-04T10:11:30"));

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            entityManager.createNativeQuery("truncate table user_site cascade")
                    .executeUpdate();
        });

        seedWithNonExpired();
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    @Test
    public void testResetLastDataFetch() {
        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(Instant.now(clock)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affected = maintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, siteId);
        assertThat(affected).hasSize(31);

        maintenanceRepository.getUserSitesForClient(clientId)
                .forEach(userSite -> {
                    assertThat(userSite.getClientId()).isEqualTo(clientId);
                    assertThat(userSite.getSiteId()).isEqualTo(siteId);
                    assertThat(userSite.getLastDataFetch()).isNull();
                });
    }

    @Test
    public void testResetLastDataFetchIncorrectClient() {
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(Instant.now(clock)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affected = maintenanceRepository.resetLastDataFetchForSiteLast180Days(ClientId.random(), siteId);
        assertThat(affected).isEmpty();
    }

    @Test
    public void testResetLastDataFetchIncorrectSite() {
        ClientId clientId = ClientId.random();

        bulkPersistPostgresUserSites(31, clientId, randomUUID(), UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(Instant.now(clock)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affected = maintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, randomUUID());
        assertThat(affected).isEmpty();
    }

    @Test
    public void testResetLastDataFetchCreateOrUpdated180DaysAgo() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .connectionStatus(ConnectionStatus.STEP_NEEDED)
                        .statusTimeoutTime(clock.instant().minusSeconds(1))
                        .created(clock.instant().minus(240, ChronoUnit.DAYS))
                        .updated(clock.instant().minus(180, ChronoUnit.DAYS)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affectedUserSites = maintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, siteId);
        assertThat(affectedUserSites).hasSize(31);
    }

    @Test
    public void testResetLastDataFetchCreateOrUpdatedLessThen180DaysAgo() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .connectionStatus(ConnectionStatus.STEP_NEEDED)
                        .statusTimeoutTime(clock.instant().minusSeconds(1))
                        .created(clock.instant().minus(240, ChronoUnit.DAYS))
                        .updated(clock.instant().minus(175, ChronoUnit.DAYS)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affectedUserSites = maintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, siteId);
        assertThat(affectedUserSites).hasSize(31);
    }

    @Test
    public void testResetLastDataFetchCreateOrUpdatedMoreThen180DaysAgo() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .connectionStatus(ConnectionStatus.STEP_NEEDED)
                        .statusTimeoutTime(clock.instant().minusSeconds(1))
                        .created(clock.instant().minus(240, ChronoUnit.DAYS))
                        .updated(clock.instant().minus(185, ChronoUnit.DAYS)), userSite -> repository.save(userSite));

        List<SimpleUserSiteId> affectedUserSites = maintenanceRepository.resetLastDataFetchForSiteLast180Days(clientId, siteId);
        assertThat(affectedUserSites).isEmpty();
    }

    void seedWithNonExpired() {

        // not expired
        bulkPersistPostgresUserSites(10, ClientId.random(), randomUUID(), UUID::randomUUID,
                (builder, i) -> builder
                        .lastDataFetch(Instant.now(clock))
                        .connectionStatus(random(ConnectionStatus.values()))
                        .statusTimeoutTime(null)
                        .created(clock.instant().minus(60, ChronoUnit.MINUTES))
                        .updated(clock.instant().minus(1, ChronoUnit.MINUTES)), userSite -> repository.save(userSite));
    }

    public static <T> T random(T[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }
}
