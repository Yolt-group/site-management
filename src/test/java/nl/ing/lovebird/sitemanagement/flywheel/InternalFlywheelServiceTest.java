package nl.ing.lovebird.sitemanagement.flywheel;

import brave.Tracer;
import brave.Tracing;
import brave.handler.SpanHandler;

import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalFlywheelServiceTest {

    @Mock
    private InternalFlywheelProperties properties;
    @Mock
    private UserService userService;
    @Mock
    private PostgresUserSiteRepository postgresUserSiteRepository;
    @Mock
    private SiteService siteService;
    @Mock
    private InternalFlywheelRefreshService internalFlywheelRefreshService;

    private UserRefreshProperties userRefreshProperties = new UserRefreshProperties();
    private InternalFlywheelService subject;

    private static Tracer tracer = Tracing.newBuilder().addSpanHandler(new SpanHandler() {}).build().tracer();

    @BeforeEach
    void beforeEach() {
        userRefreshProperties.setRefreshesPerDay(new HashMap<>());
        userRefreshProperties.setDefaultRefreshesPerDay(1);
        FlywheelUserUUIDRangeSelector flywheelUserUUIDRangeSelector = new FlywheelUserUUIDRangeSelector();
        subject = new InternalFlywheelService(properties, userService, postgresUserSiteRepository, tracer, siteService, internalFlywheelRefreshService,
                userRefreshProperties, flywheelUserUUIDRangeSelector);
    }

    @Test
    void refreshUserSitesAsync_flywheelEnabledAndMixOfUsers_refreshesApplicableUsers() {
        var now = LocalTime.now();
        var activeUserId = UUID.randomUUID();
        var blockedUserId = UUID.randomUUID();
        var activeAisUserIdNotFetchedBefore = UUID.randomUUID();
        var activeYoltAppUserId = UUID.randomUUID();
        var clientId = new ClientId(UUID.randomUUID());

        when(properties.isEnabled()).thenReturn(true);
        when(postgresUserSiteRepository.getClientIdsWithUserSite()).thenReturn(Set.of(clientId, ClientIds.YTS_CREDIT_SCORING_APP));
        when(postgresUserSiteRepository.getUserIdsBetween(any(), any(), eq(clientId))).thenReturn(List.of(activeUserId, blockedUserId, activeAisUserIdNotFetchedBefore));
        when(postgresUserSiteRepository.getUserIdsBetween(any(), any(), eq(ClientIds.YTS_CREDIT_SCORING_APP))).thenReturn(List.of(activeYoltAppUserId));

        when(userService.getUser(activeUserId)).thenReturn(Optional.of(new User(activeUserId, Instant.now().minus(11, ChronoUnit.DAYS), clientId, StatusType.ACTIVE, false)));
        when(userService.getUser(blockedUserId)).thenReturn(Optional.of(new User(blockedUserId, null, clientId, StatusType.BLOCKED, false)));
        when(userService.getUser(activeAisUserIdNotFetchedBefore)).thenReturn(Optional.of(new User(activeAisUserIdNotFetchedBefore, null, clientId, StatusType.ACTIVE, true)));
        when(userService.getUser(activeYoltAppUserId)).thenReturn(Optional.of(new User(activeYoltAppUserId, Instant.now().minus(2, ChronoUnit.DAYS), ClientIds.YTS_CREDIT_SCORING_APP, StatusType.ACTIVE, false)));

        subject.refreshUserSitesAsync(now);

        verify(internalFlywheelRefreshService).refreshForUser(activeUserId, false, false);
        verify(internalFlywheelRefreshService).refreshForUser(activeAisUserIdNotFetchedBefore, true, false);
        verify(internalFlywheelRefreshService).refreshForUser(activeYoltAppUserId, false, false);

    }

    @Test
    void refreshUserSitesAsync_flywheelEnabledAndUserNotFound_doesNothing() {
        var now = LocalTime.now();
        var userId = UUID.randomUUID();
        when(properties.isEnabled()).thenReturn(true);
        when(postgresUserSiteRepository.getClientIdsWithUserSite()).thenReturn(Set.of(ClientIds.TEST_CLIENT));
        when(postgresUserSiteRepository.getUserIdsBetween(any(), any(), eq(ClientIds.TEST_CLIENT))).thenReturn(List.of(userId));

        when(userService.getUser(userId)).thenReturn(Optional.empty());

        subject.refreshUserSitesAsync(now);

    }

    @Test
    void refreshUserSitesAsync_flywheelEnabledAndBlockedUser_doesNothing() {
        var now = LocalTime.now();
        var userId = UUID.randomUUID();
        var clientId = new ClientId(UUID.randomUUID());

        when(properties.isEnabled()).thenReturn(true);
        when(postgresUserSiteRepository.getClientIdsWithUserSite()).thenReturn(Set.of(clientId));
        when(postgresUserSiteRepository.getUserIdsBetween(any(), any(), eq(clientId))).thenReturn(List.of(userId));
        when(userService.getUser(userId)).thenReturn(Optional.of(new User(userId, null,
                clientId, StatusType.BLOCKED, false)));

        subject.refreshUserSitesAsync(now);

        verifyNoMoreInteractions(properties, userService, siteService, internalFlywheelRefreshService);
    }

    @Test
    void refreshUserSitesAsync_flywheelDisabled_doesNothing() {
        when(properties.isEnabled()).thenReturn(false);

        subject.refreshUserSitesAsync(LocalTime.now());

        verifyNoMoreInteractions(properties, userService, siteService, internalFlywheelRefreshService);
    }

    @Test
    void refreshUserSitesAsync_encountersRuntimeExceptionBeforeProcessingUsers_swallowsTheException() {
        when(properties.isEnabled()).thenThrow(new RuntimeException());

        subject.refreshUserSitesAsync(LocalTime.now());

        verifyNoMoreInteractions(properties, userService, siteService, internalFlywheelRefreshService);
    }

    @Test
    void forceRefreshUserSitesForSpecificUserAsync_existingUser_triggersRefreshWithForceFlagSetToTrue() {
        var userId = UUID.randomUUID();

        when(userService.getUserOrThrow(userId)).thenReturn(new User(userId, null, new ClientId(UUID.randomUUID()), StatusType.ACTIVE, false));

        subject.forceRefreshUserSitesForSpecificUserAsync(userId);

        verify(internalFlywheelRefreshService).refreshForUser(userId, false, true);
        verifyNoMoreInteractions(properties, userService, siteService, internalFlywheelRefreshService);
    }

    @Test
    void forceRefreshUserSitesForSpecificUserAsync_userDoesNotExist_swallowsTheException() {
        var userId = UUID.randomUUID();

        when(userService.getUserOrThrow(userId)).thenThrow(new IllegalArgumentException());

        subject.forceRefreshUserSitesForSpecificUserAsync(userId);

        verifyNoMoreInteractions(properties, userService, siteService, internalFlywheelRefreshService);
    }

    @Test
    void when_refreshing_ItShouldNotCrashForAnyMinuteOfTheDay_And_ShouldAlwaysRequestAProperRange() {
        ClientId clientId = ClientId.random();
        for (LocalDateTime localDateTime = LocalDateTime.MIN.plusSeconds(10); localDateTime.getDayOfMonth() == LocalDateTime.MIN.getDayOfMonth(); localDateTime = localDateTime.plusMinutes(1)) {
            subject.usersToRefreshForClient(clientId, localDateTime.toLocalTime());
            ArgumentCaptor<UUID> lowerBound = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> upperBound = ArgumentCaptor.forClass(UUID.class);
            verify(postgresUserSiteRepository).getUserIdsBetween(lowerBound.capture(), upperBound.capture(), eq(clientId));
            // This next assertion is really wonky... because comparison of UUID in postgres and Java works in a different way.
            // In postgress uuid('802d82d8-2d82-d82d-82d8-2d82d82d8258') > uuid('7fffffff-ffff-ffff-ffff-ffffffffff80') = true.
            // In java, UUID.fromString("802d82d8-2d82-d82d-82d8-2d82d82d8258").compareTo(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffff80")) = -1
            // This is because in java the most significant bits (repr. as long) flip to negative at some point, making it small than.
            // https://bugs.openjdk.java.net/browse/JDK-7025832
            assertThat(new BigInteger(lowerBound.getValue().toString().replace("-", ""), 16).compareTo(
                    new BigInteger(upperBound.getValue().toString().replace("-", ""), 16))).isEqualTo(-1);
            Mockito.clearInvocations(postgresUserSiteRepository);
        }
    }

}