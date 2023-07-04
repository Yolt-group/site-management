package nl.ing.lovebird.sitemanagement.flywheel;

import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistPostgresUserSites;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


@Disabled("This test is disabled because it takes too much time to run.  The test simulates a full day of a flywheel run and proves that everything works. You can run it in case you need to troubleshoot something")
//@IntegrationTestContext
class InternalFlywheelServiceIntegrationTest {

    @Autowired
    private UserRefreshProperties userRefreshProperties;

    @Autowired
    private PostgresUserSiteRepository postgresUserSiteRepository;

    @Autowired
    private InternalFlywheelProperties internalFlywheelProperties;

    @Autowired
    private brave.Tracer braveTracer;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final UserService mockUserService = mock(UserService.class);
    private final SiteService mockSiteService = mock(SiteService.class);
    private final InternalFlywheelRefreshService internalFlywheelRefreshService = mock(InternalFlywheelRefreshService.class);

    private InternalFlywheelService internalFlywheelService;

    @BeforeEach
    public void beforeEach() {

        // Remove transactions
        transactionTemplate.executeWithoutResult(transactionStatus -> entityManager.createNativeQuery("truncate table user_site cascade")
                .executeUpdate());

        // We don't care about the user, as long as it exists and not blocked.
        lenient().when(mockUserService.getUser(any())).thenAnswer(a -> {
            UUID userId = a.getArgument(0);
            return Optional.of(new User(userId, Instant.now(), ClientId.random(), StatusType.ACTIVE, false));
        });
    }

    /**
     * This is explicitly cramped into 1 test instead of using a parameterized test, because it's quite costly to simulate the flywheel for 1 day.
     * This is due to the fact that it runs 1440 (24 * 60) minutes/times.
     * In each time it does a couple of queries:
     * - get all clients
     * - for each client get the user-ids
     */
//    @Test
    @Disabled("this test took too much time, although it is actually proof that the flywheel works. You can run it in case you need to troubleshoot something")
    void when_TheClientConfigStatesNRefreshesPerDay_then_theFlywheelShouldRefreshAUserNTimesADay() {

        ClientId client1RefreshADay = new ClientId(UUID.randomUUID());
        UUID userIdClient1 = UUID.randomUUID();
        ClientId client2RefreshADay = new ClientId(UUID.randomUUID());
        UUID userIdClient2 = UUID.randomUUID();
        ClientId client3RefreshADay = new ClientId(UUID.randomUUID());
        UUID userIdClient3 = UUID.randomUUID();
        ClientId client4RefreshADay = new ClientId(UUID.randomUUID());
        UUID userIdClient4 = UUID.randomUUID();

        userRefreshProperties.getRefreshesPerDay().put(client1RefreshADay.unwrap(), 1);
        userRefreshProperties.getRefreshesPerDay().put(client2RefreshADay.unwrap(), 2);
        userRefreshProperties.getRefreshesPerDay().put(client3RefreshADay.unwrap(), 3);
        userRefreshProperties.getRefreshesPerDay().put(client4RefreshADay.unwrap(), 4);
        internalFlywheelService = new InternalFlywheelService(internalFlywheelProperties, mockUserService, postgresUserSiteRepository,
                braveTracer, mockSiteService, internalFlywheelRefreshService, userRefreshProperties, new FlywheelUserUUIDRangeSelector());

        bulkPersistPostgresUserSites(2, client1RefreshADay, randomUUID(), () -> userIdClient1,
                (builder, i) -> builder, userSite -> postgresUserSiteRepository.save(userSite));
        bulkPersistPostgresUserSites(2, client2RefreshADay, randomUUID(), () -> userIdClient2,
                (builder, i) -> builder, userSite -> postgresUserSiteRepository.save(userSite));
        bulkPersistPostgresUserSites(2, client3RefreshADay, randomUUID(), () -> userIdClient3,
                (builder, i) -> builder, userSite -> postgresUserSiteRepository.save(userSite));
        bulkPersistPostgresUserSites(2, client4RefreshADay, randomUUID(), () -> userIdClient4,
                (builder, i) -> builder, userSite -> postgresUserSiteRepository.save(userSite));

        simulateFlywheelForADay();

        verify(internalFlywheelRefreshService, times(1)).refreshForUser(eq(userIdClient1), anyBoolean(), anyBoolean());
        verify(internalFlywheelRefreshService, times(2)).refreshForUser(eq(userIdClient2), anyBoolean(), anyBoolean());
        verify(internalFlywheelRefreshService, times(3)).refreshForUser(eq(userIdClient3), anyBoolean(), anyBoolean());
        verify(internalFlywheelRefreshService, times(4)).refreshForUser(eq(userIdClient4), anyBoolean(), anyBoolean());
    }

//    @Test
    @Disabled("ignored as it takes too much time to run this and insert all those user sites. You can play around with it locally if you like.")
    void when_theFlywheelIsEnabled_then_AllUsersWithAUserSiteShouldBeRefreshedAtLeastOnceADay() {

        Set<UUID> userIdsWithUserSite = new HashSet<>();
        // when 500 user sites with unique user_id for TEST_CLIENT
        bulkPersistPostgresUserSites(500, ClientIds.TEST_CLIENT, randomUUID(), UUID::randomUUID,
                (builder, i) -> builder, userSite -> {
                    postgresUserSiteRepository.save(userSite);
                    userIdsWithUserSite.add(userSite.getUserId());
                    return userSite;
                });

        // when 500 user sites with unique user_id
        bulkPersistPostgresUserSites(500, ClientIds.ACCOUNTING_CLIENT, randomUUID(), UUID::randomUUID,
                (builder, i) -> builder, userSite -> {
                    postgresUserSiteRepository.save(userSite);
                    userIdsWithUserSite.add(userSite.getUserId());
                    return userSite;
                });

        // when 500 user sites with 1 user_id
        UUID userIdWith500UserSites = UUID.randomUUID();
        bulkPersistPostgresUserSites(500, ClientIds.ACCOUNTING_CLIENT, randomUUID(), () -> userIdWith500UserSites,
                (builder, i) -> builder, userSite -> {
                    postgresUserSiteRepository.save(userSite);
                    userIdsWithUserSite.add(userSite.getUserId());
                    return userSite;
                });
        // 1001 users in total.
        assertThat(userIdsWithUserSite.size()).isEqualTo(1001);

        // We now expect 501 * 3 + 500 * 4 = 3503 refreshes for a day.
        userRefreshProperties.getRefreshesPerDay().put(ClientIds.ACCOUNTING_CLIENT.unwrap(), 3);
        userRefreshProperties.getRefreshesPerDay().put(ClientIds.TEST_CLIENT.unwrap(), 4);
        internalFlywheelService = new InternalFlywheelService(internalFlywheelProperties, mockUserService, postgresUserSiteRepository,
                braveTracer, mockSiteService, internalFlywheelRefreshService, userRefreshProperties, new FlywheelUserUUIDRangeSelector());


        simulateFlywheelForADay();

        ArgumentCaptor<UUID> userIdCapture = ArgumentCaptor.forClass(UUID.class);
        verify(internalFlywheelRefreshService, times(3503)).refreshForUser(userIdCapture.capture(), anyBoolean(), anyBoolean());
        Assertions.assertThat(Sets.newHashSet(userIdCapture.getAllValues())).containsExactlyInAnyOrderElementsOf(userIdsWithUserSite);
    }

    private void simulateFlywheelForADay() {
        // Batch trigger calls SM every minute with a 10 second offset. so 00:00:10 --> 00:01:10 --> .. 23:59:10
        // This is to make sure that we always have a window with rounded minutes.
        for (LocalDateTime localDateTime = LocalDateTime.MIN.plusSeconds(10); localDateTime.getDayOfMonth() == LocalDateTime.MIN.getDayOfMonth(); localDateTime = localDateTime.plusMinutes(1)) {
            internalFlywheelService.processRefreshesForCurrentMinute(localDateTime.toLocalTime());
        }
    }
}