package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import nl.ing.lovebird.sitemanagement.lib.MutableClock;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistPostgresUserSites;
import static org.assertj.core.api.Assertions.assertThat;

@TestContainerDataJpaTest
public class PostgresUserSiteLockRepositoryTest {

    private static final LocalDateTime FIXED_POINT_IN_TIME = LocalDateTime.of(2018, 9, 25, 12, 1, 11);

    @Autowired
    PostgresUserSiteRepository userSiteRepository;

    @Autowired
    PostgresUserSiteLockRepository userSiteLockRepository;

    @Autowired
    MutableClock clock;

    @BeforeEach
    public void setUp() {
        clock.asFixed(FIXED_POINT_IN_TIME);
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    @Test
    void testGetAnyCondition() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();

        // create user-site
        createPostgresUserSite(userId, userSiteId);

        // assert there is no lock for the user-site
        Optional<PostgresUserSiteLock> l1 = userSiteLockRepository.getUnconditionally(userSiteId);
        assertThat(l1).isEmpty();

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, randomUUID());
        assertThat(isLocked).isTrue();

        Optional<PostgresUserSiteLock> l2 = userSiteLockRepository.getUnconditionally(userSiteId);
        assertThat(l2).isNotEmpty();
    }

    @Test
    void testInsertLock() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        // assert there is no lock
        Optional<PostgresUserSiteLock> postgresUserSiteLock = userSiteLockRepository.getUnconditionally(userSiteId);
        assertThat(postgresUserSiteLock).isEmpty();

        // create a lock by insert (no previously existing lock)
        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();
    }

    @Test
    void testLockAndUnlock() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();

        boolean isUnlocked = userSiteLockRepository.unlockUserSite(userSiteId);
        assertThat(isUnlocked).isTrue();

        Optional<PostgresUserSiteLock> postgresUserSiteLock = userSiteLockRepository.get(userSiteId);
        assertThat(postgresUserSiteLock).isEmpty();
    }

    @Test
    void shouldFailLockWhenAlreadyLocked() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();

        boolean isLockedAgain = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLockedAgain).isFalse();
    }

    @Test
    void shouldSucceedLockWhenLockExpiredAfter10minutesOrMore() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();

        // move time 10 minutes in the future
        clock.asFixed(FIXED_POINT_IN_TIME.plus(10, ChronoUnit.MINUTES));

        boolean isLockedAgain = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLockedAgain).isTrue();
    }

    @Test
    void shouldFailLockWhenIsNotExpiredAfter9MinutesOrLess() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();

        // move time 9 minutes in the future
        clock.asFixed(FIXED_POINT_IN_TIME.plus(9, ChronoUnit.MINUTES));

        boolean isLockedAgain = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLockedAgain).isFalse();
    }

    @Test
    void shouldGetLockWhileLocked() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();

        PostgresUserSiteLock postgresUserSiteLock = userSiteLockRepository.get(userSiteId)
                .orElseThrow(() -> new AssertionError("user-site should be locked but no locks found."));
        assertThat(postgresUserSiteLock)
                .isEqualTo(new PostgresUserSiteLock(userSiteId, activityId, Instant.now(clock).truncatedTo(ChronoUnit.MILLIS)));
    }

    @Test
    void shouldNotGetLockWhileNotLocked() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        assertThat(userSiteLockRepository.get(userSiteId)).isEmpty();
    }

    @Test
    void shouldDeleteLockWhenUserSiteIsDeleted() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();

        createPostgresUserSite(userId, userSiteId);

        boolean isLocked = userSiteLockRepository.attemptLock(userSiteId, activityId);
        assertThat(isLocked).isTrue();
        assertThat(userSiteLockRepository.get(userSiteId)).isNotEmpty();

        deletePostgresUserSite(userSiteId);
        assertThat(userSiteLockRepository.get(userSiteId)).isEmpty();
    }


    private void createPostgresUserSite(final UUID userId, final UUID userSiteId) {
        bulkPersistPostgresUserSites(1, ClientId.random(), randomUUID(), () -> userId,
                (builder, i) -> builder
                        .userSiteId(userSiteId), u -> userSiteRepository.save(u));
    }

    private void deletePostgresUserSite(final UUID userSiteId) {
        userSiteRepository.deleteUserSite(userSiteId);
    }
}
