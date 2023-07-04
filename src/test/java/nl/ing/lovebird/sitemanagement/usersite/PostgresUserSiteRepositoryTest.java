package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import nl.ing.lovebird.sitemanagement.flywheel.FlywheelUserUUIDRangeSelectorTest;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository.UserSiteConnectionInfo;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestContainerDataJpaTest
class PostgresUserSiteRepositoryTest {

    @Autowired
    PostgresUserSiteRepository repository;

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
    }

    @Test
    void shouldSavePostgresUserSite() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();
        UUID userId = randomUUID();
        PostgresUserSite unsavedUserSite = createRandomPostgresUserSite(clientId, siteId, userId);

        PostgresUserSite userSite = repository.save(unsavedUserSite);
        assertThat(userSite).isEqualTo(unsavedUserSite);
    }

    @Test
    void shouldSavePostgreUserSiteNonNullableFields() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();
        UUID userId = randomUUID();

        PostgresUserSite unsavedUserSite = PostgresUserSite.builder()
                .userSiteId(randomUUID())
                .userId(userId)
                .siteId(siteId)
                .connectionStatus(ConnectionStatus.CONNECTED)
                .created(Instant.now(clock))
                .clientId(clientId.unwrap())
                .provider("STARLINGBANK")
                .redirectUrlId(randomUUID())
                .build();

        PostgresUserSite userSite = repository.save(unsavedUserSite);
        assertThat(userSite).isEqualTo(unsavedUserSite);
    }

    @Test
    void testGetUserSiteWithWriteLock() {
        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        List<PostgresUserSite> postgresUserSites = bulkPersistPostgresUserSites(21, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder.connectionStatus(ConnectionStatus.CONNECTED), postgresUserSite -> repository.save(postgresUserSite));

        postgresUserSites.forEach(postgresUserSite -> {
            PostgresUserSite userSite = repository.getUserSiteWithWriteLock(postgresUserSite.getUserId(), postgresUserSite.getUserSiteId())
                    .orElseThrow(() -> new AssertionError("PostgresUserSite not available."));

            // replace with equalsTo after https://git.yolt.io/backend/site-management/-/merge_requests/2570
            assertThat(userSite.getUserSiteId()).isEqualTo(postgresUserSite.getUserSiteId());
        });
    }

    @Test
    void testPostgreSQLEnforceDeleteConstraintTriggerDeletedAtNull() {

        PostgresUserSite userSite = bulkPersistPostgresUserSites(1, ClientId.random(), randomUUID(), UUID::randomUUID,
                (builder, i) -> builder.isDeleted(false), postgresUserSite -> repository.save(postgresUserSite)).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionFailure("Failed to save user-site"));

        userSite.setDeleted(true);
        userSite.setDeletedAt(null); // enforce_deleted_constraints_fn violation

        assertThatThrownBy(() -> {
            repository.save(userSite);
        }).hasRootCauseInstanceOf(PSQLException.class)
                .hasRootCauseMessage("ERROR: The deleted timestamp is required when a user-site is marked as deleted.\n" +
                        "  Where: PL/pgSQL function enforce_deleted_constraints_fn() line 4 at RAISE");
    }

    @Test
    void testPostgreSQLEnforceDeleteConstraintTriggerUnmarkDelete() {

        PostgresUserSite userSite = bulkPersistPostgresUserSites(1, ClientId.random(), randomUUID(), UUID::randomUUID,
                (builder, i) -> builder.isDeleted(true).deletedAt(Instant.now(clock)), postgresUserSite -> repository.save(postgresUserSite)).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionFailure("Failed to save user-site"));

        userSite.setDeleted(false); // enforce_deleted_constraints_fn violation
        userSite.setDeletedAt(Instant.now(clock));

        assertThatThrownBy(() -> {
            repository.save(userSite);
        }).hasRootCauseInstanceOf(PSQLException.class)
                .hasRootCauseMessage("ERROR: The user-site cannot be un-marked as deleted.\n" +
                        "  Where: PL/pgSQL function enforce_deleted_constraints_fn() line 8 at RAISE");
    }

    @Test
    void shouldReturnNrOfConnectedUsers() {
        ClientId clientId = ClientId.random();
        UUID siteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, () -> userId,
                (builder, i) -> builder.connectionStatus(ConnectionStatus.CONNECTED), postgresUserSite -> repository.save(postgresUserSite));

        assertThat(repository.getUserSiteTotalsInfo(clientId))
                .containsEntry(siteId, PostgresUserSiteRepository.UserSiteTotalsInfo.builder()
                        .siteId(siteId)
                        .nrOfUniqueConnections(31)
                        .nrOfUniqueUsers(1)
                        .build());
    }

    @Test
    void shouldReturnConnectionStateTotals() {

        ClientId clientId = ClientId.random();
        UUID siteId = randomUUID();

        bulkPersistPostgresUserSites(31, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder.lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .connectionStatus(ConnectionStatus.CONNECTED), postgresUserSite -> repository.save(postgresUserSite));

        bulkPersistPostgresUserSites(21, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .connectionStatus(ConnectionStatus.DISCONNECTED), postgresUserSite -> repository.save(postgresUserSite));

        bulkPersistPostgresUserSites(11, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .connectionStatus(ConnectionStatus.CONNECTED)
                        .failureReason(FailureReason.TECHNICAL_ERROR), postgresUserSite -> repository.save(postgresUserSite));

        bulkPersistPostgresUserSites(1, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .connectionStatus(ConnectionStatus.STEP_NEEDED), postgresUserSite -> repository.save(postgresUserSite));

        Map<UUID, List<UserSiteConnectionInfo>> connectionStateTotals = repository.getConnectionStatusBySite(clientId);

        assertThat(connectionStateTotals).containsKey(siteId);
        assertThat(connectionStateTotals.get(siteId)).containsExactlyInAnyOrder(
                UserSiteConnectionInfo.builder()
                        .siteId(siteId)
                        .connectionStatus(ConnectionStatus.CONNECTED)
                        .count(31)
                        .build(),
                UserSiteConnectionInfo.builder()
                        .siteId(siteId)
                        .connectionStatus(ConnectionStatus.DISCONNECTED)
                        .count(21)
                        .build(),
                UserSiteConnectionInfo.builder()
                        .siteId(siteId)
                        .connectionStatus(ConnectionStatus.CONNECTED)
                        .failureReason(FailureReason.TECHNICAL_ERROR)
                        .count(11)
                        .build(),
                UserSiteConnectionInfo.builder()
                        .siteId(siteId)
                        .connectionStatus(ConnectionStatus.STEP_NEEDED)
                        .count(1)
                        .build()
        );
    }

    @Test
    void shouldOnlyReturnStatisticsForClient() {

        ClientId clientId = ClientId.random();
        UUID siteId = UUID.randomUUID();

        // insert user-sites for `client-id`
        bulkPersistPostgresUserSites(5, clientId, siteId, UUID::randomUUID,
                (builder, i) -> builder
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .connectionStatus(ConnectionStatus.STEP_NEEDED), postgresUserSite -> repository.save(postgresUserSite));

        // insert user-sites for other (random) clients
        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));

        Map<UUID, List<UserSiteConnectionInfo>> connectionStatusBySiteForClient = repository.getConnectionStatusBySite(clientId);

        assertThat(connectionStatusBySiteForClient.get(siteId)).containsExactlyInAnyOrder(
                UserSiteConnectionInfo.builder()
                        .siteId(siteId)
                        .connectionStatus(ConnectionStatus.STEP_NEEDED)
                        .count(5)
                        .build());
    }

    @Test
    void shouldReturnCountsOfUniqueUsersWithActiveUserSitePerClient() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        ClientId clientId1 = new ClientId(UUID.randomUUID());
        ClientId clientId2 = new ClientId(UUID.randomUUID());

        // 2 user sites of user1 client1
        bulkPersistPostgresUserSites(31, clientId1, UUID.randomUUID(), () -> userId1, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(31, clientId1, UUID.randomUUID(), () -> userId1, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));

        // 1 user site of user2 client1
        bulkPersistPostgresUserSites(31, clientId1, UUID.randomUUID(), () -> userId2, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));

        // 1 user site of user3 client2
        bulkPersistPostgresUserSites(31, clientId2, UUID.randomUUID(), () -> userId3, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));

        Set<ClientId> userCountsWithActiveUserSitesByClientId = repository.getClientIdsWithUserSite();

        assertThat(userCountsWithActiveUserSitesByClientId).containsExactlyInAnyOrder(
                clientId1, clientId2
        );

    }

    @Test
    void when_queryingForUserIdsBetweenMinUUIDAndMaxUUID_then_shouldReturnAllUserIds() {
        bulkPersistPostgresUserSites(10, ClientIds.TEST_CLIENT, UUID.randomUUID(), UUID::randomUUID, (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));

        List<UUID> userIdsBetween = repository.getUserIdsBetween(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                ClientIds.TEST_CLIENT);

        assertThat(userIdsBetween).hasSize(10);
    }

    @Test
    void when_queryingForUserIdsBetweenUUID_then_shouldReturnAllUserIds() {
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000002"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000003"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000004"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));
        bulkPersistPostgresUserSites(1, ClientIds.TEST_CLIENT, UUID.randomUUID(), () -> UUID.fromString("00000000-0000-0000-0000-000000000005"), (builder, i) -> builder, postgresUserSite -> repository.save(postgresUserSite));


        List<UUID> userIdsBetween = repository.getUserIdsBetween(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), // Inclusive
                UUID.fromString("00000000-0000-0000-0000-000000000004"),// Exclusive
                ClientIds.TEST_CLIENT);

        assertThat(userIdsBetween).hasSize(4);
    }

    /**
     * See {@link nl.ing.lovebird.sitemanagement.flywheel.FlywheelUserUUIDRangeSelectorTest#uuidCompare}
     */
    @Test
    void given_javaScrewedUpUUIDComparison_then_weCheckThatPostgressDoesntSinceWeRelyOnThatBigTime() {
        UUID smallerUUID = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffff80");
        UUID inbetweenUUID = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffff81");
        UUID thebiggerUUID = UUID.fromString("802d82d8-2d82-d82d-82d8-2d82d82d8258");
        PostgresUserSite randomUserSite = createRandomUserSite(ClientIds.TEST_CLIENT, randomUUID(), inbetweenUUID);
        repository.save(randomUserSite);



        List<UUID> userIdsBetween = repository.getUserIdsBetween(smallerUUID, thebiggerUUID, ClientIds.TEST_CLIENT);

        // strange.
        assertThat(smallerUUID).isGreaterThan(thebiggerUUID);

        assertThat(FlywheelUserUUIDRangeSelectorTest.uuidCompare(smallerUUID, thebiggerUUID)).isEqualTo(-1);
        // Luckily
        assertThat(userIdsBetween).containsExactly(inbetweenUUID);

    }
}
