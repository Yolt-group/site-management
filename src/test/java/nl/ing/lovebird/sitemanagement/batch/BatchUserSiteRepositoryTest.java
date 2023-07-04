package nl.ing.lovebird.sitemanagement.batch;

import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistPostgresUserSites;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.createRandomUserSite;
import static org.assertj.core.api.Assertions.assertThat;

@TestContainerDataJpaTest
class BatchUserSiteRepositoryTest {

    @Autowired
    PostgresUserSiteRepository repository;

    @Autowired
    BatchUserSiteRepository batchUserSiteRepository;

    @Autowired
    Clock clock;

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    public void cleanUp() {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            entityManager.createNativeQuery("truncate table user_site cascade")
                    .executeUpdate();
        });

        seedWithNonExpired();
    }

    @Test
    public void testDisconnectUserSitesForSite() {
        final var clientId = ClientId.random();
        final var siteId1 = randomUUID();
        final var siteId2 = randomUUID();

        final var userSite1 = repository.save(createRandomUserSite(clientId, siteId1, randomUUID()));
        final var userSite2 = repository.save(createRandomUserSite(clientId, siteId1, randomUUID()));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSite(clientId, siteId1, randomUUID())));
        final var userSite4 = repository.save(createRandomUserSite(clientId, siteId2, randomUUID()));

        final var result = batchUserSiteRepository.disconnectUserSitesForSite(siteId1, false);

        assertThat(result).isEqualTo(2L);

        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(disconnectedByRepository(userSite1));
        assertThat(repository.getUserSite(userSite2.getUserId(), userSite2.getUserSiteId())).contains(disconnectedByRepository(userSite2));
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(userSite4);
    }

    @Test
    public void testDisconnectUserSitesForSiteDryRun() {
        final var clientId = ClientId.random();
        final var siteId1 = randomUUID();
        final var siteId2 = randomUUID();

        final var userSite1 = repository.save(createRandomUserSite(clientId, siteId1, randomUUID()));
        final var userSite2 = repository.save(createRandomUserSite(clientId, siteId1, randomUUID()));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSite(clientId, siteId1, randomUUID())));
        final var userSite4 = repository.save(createRandomUserSite(clientId, siteId2, randomUUID()));

        final var result = batchUserSiteRepository.disconnectUserSitesForSite(siteId1, true);

        assertThat(result).isEqualTo(2L);

        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(userSite1);
        assertThat(repository.getUserSite(userSite2.getUserId(), userSite2.getUserSiteId())).contains(userSite2);
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(userSite4);
    }

    @Test
    public void testDisconnectUserSitesWithMigrationStatus() {
        final var clientId = ClientId.random();
        final var siteId = randomUUID();

        final var userSite1 = repository.save(createRandomUserSite(clientId, siteId, randomUUID()));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_TO)));
        final var userSite4 = repository.save(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_TO));
        final var userSite5 = repository.save(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_FROM));

        final var result = batchUserSiteRepository.disconnectUserSitesWithMigrationStatus(List.of(MigrationStatus.MIGRATING_TO, MigrationStatus.MIGRATING_FROM), false);

        assertThat(result).isEqualTo(2L);

        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(userSite1);
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(disconnectedByRepository(userSite4));
        assertThat(repository.getUserSite(userSite5.getUserId(), userSite5.getUserSiteId())).contains(disconnectedByRepository(userSite5));
    }

    @Test
    public void testDisconnectUserSitesWithMigrationStatusDryRun() {
        final var clientId = ClientId.random();
        final var siteId = randomUUID();

        final var userSite1 = repository.save(createRandomUserSite(clientId, siteId, randomUUID()));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_TO)));
        final var userSite4 = repository.save(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_TO));
        final var userSite5 = repository.save(createRandomUserSiteWithMigrationStatus(clientId, siteId, randomUUID(), MigrationStatus.MIGRATING_FROM));

        final var result = batchUserSiteRepository.disconnectUserSitesWithMigrationStatus(List.of(MigrationStatus.MIGRATING_TO, MigrationStatus.MIGRATING_FROM), true);

        assertThat(result).isEqualTo(2L);

        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(userSite1);
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(userSite4);
        assertThat(repository.getUserSite(userSite5.getUserId(), userSite5.getUserSiteId())).contains(userSite5);
    }

    @Test
    public void testDisconnectUserSitesNotRefreshedFor90Days() {
        final var clientId = ClientId.random();
        final var siteId = randomUUID();

        final var t = Instant.now(clock);
        final var tMin90d = t.minus(90, DAYS);

        final var userSite1 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), t, tMin90d.minusSeconds(60)));
        final var userSite2 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.plusSeconds(60), tMin90d.minusSeconds(60)));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.minusSeconds(60))));
        final var userSite4 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.plusSeconds(60)));
        final var userSite5 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), null, tMin90d.plusSeconds(60)));
        final var userSite6 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), null, tMin90d.minusSeconds(60)));
        final var userSite7 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.minusSeconds(60)));
        final var ppsUserSite = repository.save(createRandomUserSiteWithLastDataFetch(clientId, UUID.fromString("b1fa25e2-f696-45c1-b59b-59c5fd40c175"), randomUUID(), tMin90d.minusSeconds(60), tMin90d.minusSeconds(60)));

        final var result = batchUserSiteRepository.disconnectUserSitesNotRefreshedFor90Days(false);

        assertThat(result).isEqualTo(2L);
        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(userSite1);
        assertThat(repository.getUserSite(userSite2.getUserId(), userSite2.getUserSiteId())).contains(userSite2);
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(userSite4);
        assertThat(repository.getUserSite(userSite5.getUserId(), userSite5.getUserSiteId())).contains(userSite5);
        assertThat(repository.getUserSite(userSite6.getUserId(), userSite6.getUserSiteId())).contains(disconnectedByRepository(userSite6));
        assertThat(repository.getUserSite(userSite7.getUserId(), userSite7.getUserSiteId())).contains(disconnectedByRepository(userSite7));
        // PPS userSites should not be disconnected
        assertThat(repository.getUserSite(ppsUserSite.getUserId(), ppsUserSite.getUserSiteId()))
                .contains(ppsUserSite)
                .map(PostgresUserSite::getConnectionStatus)
                .hasValue(ConnectionStatus.CONNECTED);
    }

    @Test
    public void testDisconnectUserSitesNotRefreshedFor90DaysDryRun() {
        final var clientId = ClientId.random();
        final var siteId = randomUUID();

        final var t = Instant.now(clock);
        final var tMin90d = t.minus(90, DAYS);

        final var userSite1 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), t, tMin90d.minusSeconds(60)));
        final var userSite2 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.plusSeconds(60), tMin90d.minusSeconds(60)));
        final var userSite3 = repository.save(connectionStatusDisconnected(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.minusSeconds(60))));
        final var userSite4 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.plusSeconds(60)));
        final var userSite5 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), null, tMin90d.plusSeconds(60)));
        final var userSite6 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), null, tMin90d.minusSeconds(60)));
        final var userSite7 = repository.save(createRandomUserSiteWithLastDataFetch(clientId, siteId, randomUUID(), tMin90d.minusSeconds(60), tMin90d.minusSeconds(60)));

        final var result = batchUserSiteRepository.disconnectUserSitesNotRefreshedFor90Days(true);

        assertThat(result).isEqualTo(2L);

        assertThat(repository.getUserSite(userSite1.getUserId(), userSite1.getUserSiteId())).contains(userSite1);
        assertThat(repository.getUserSite(userSite2.getUserId(), userSite2.getUserSiteId())).contains(userSite2);
        assertThat(repository.getUserSite(userSite3.getUserId(), userSite3.getUserSiteId())).contains(userSite3);
        assertThat(repository.getUserSite(userSite4.getUserId(), userSite4.getUserSiteId())).contains(userSite4);
        assertThat(repository.getUserSite(userSite5.getUserId(), userSite5.getUserSiteId())).contains(userSite5);
        assertThat(repository.getUserSite(userSite6.getUserId(), userSite6.getUserSiteId())).contains(userSite6);
        assertThat(repository.getUserSite(userSite7.getUserId(), userSite7.getUserSiteId())).contains(userSite7);
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

    private static PostgresUserSite disconnectedByRepository(PostgresUserSite userSite) {
        return userSite.toBuilder()
                .connectionStatus(ConnectionStatus.DISCONNECTED)
                .failureReason(FailureReason.CONSENT_EXPIRED)
                .build();
    }

    private static PostgresUserSite connectionStatusDisconnected(PostgresUserSite userSite) {
        return userSite.toBuilder()
                .connectionStatus(ConnectionStatus.DISCONNECTED)
                .build();
    }

    private static PostgresUserSite createRandomUserSiteWithMigrationStatus(ClientId clientId, UUID siteId, UUID userId, MigrationStatus migrationStatus) {
        return createRandomUserSite(clientId, siteId, userId).toBuilder()
                .migrationStatus(migrationStatus)
                .build();
    }

    private static PostgresUserSite createRandomUserSiteWithLastDataFetch(ClientId clientId, UUID siteId, UUID userId, Instant lastDataFetch, Instant created) {
        return createRandomUserSite(clientId, siteId, userId).toBuilder()
                .lastDataFetch(Optional.ofNullable(lastDataFetch).map(ldf -> ldf.truncatedTo(MICROS)).orElse(null))
                .created(created)
                .build();
    }
}
