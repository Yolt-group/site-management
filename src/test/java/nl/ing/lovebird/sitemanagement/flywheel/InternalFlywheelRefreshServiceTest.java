package nl.ing.lovebird.sitemanagement.flywheel;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.createYoltProviderSite;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.FLYWHEEL_REFRESH;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalFlywheelRefreshServiceTest {


    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID userSiteId = UUID.randomUUID();

    @Mock
    private UserSiteService userSiteService;
    @Mock
    private ClientTokenRequesterService clientTokenRequesterService;
    @Mock
    private UserSiteRefreshService userSiteRefreshService;
    @Mock
    private InternalFlywheelProperties properties;

    private InternalFlywheelRefreshService subject;

    private final Site yoltProviderSite = createYoltProviderSite();

    @BeforeEach
    void setUp() {
        subject = new InternalFlywheelRefreshService(systemUTC(), userSiteService, properties, clientTokenRequesterService, userSiteRefreshService);
    }

    @Test
    void refreshForUser_happyFlow_userSiteShouldBeRefreshed() {
        var clientUserToken = new ClientUserToken("client-token", null);
        var activeUserSite = createUserSite(yoltProviderSite.getId(), null);

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());
        when(clientTokenRequesterService.getClientUserToken(clientId, userId)).thenReturn(clientUserToken);

        subject.refreshForUser(userId, false, false);

        verify(userSiteRefreshService).refreshUserSitesBlocking(singletonList(activeUserSite), false,
                clientUserToken, FLYWHEEL_REFRESH, null, null);
    }

    @Test
    void refreshForUser_forceRefreshOneOffAisUserWhoseUserSiteHasntBeenFetchedBefore_userSiteShouldBeRefreshed() {
        var clientUserToken = new ClientUserToken("client-token", null);
        var activeUserSite = createUserSite(yoltProviderSite.getId(), null);

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());
        when(clientTokenRequesterService.getClientUserToken(clientId, userId)).thenReturn(clientUserToken);

        subject.refreshForUser(userId, true, true);

        verify(userSiteRefreshService).refreshUserSitesBlocking(singletonList(activeUserSite), true,
                clientUserToken, FLYWHEEL_REFRESH, null, null);
    }

    @Test
    void refreshForUser_forceRefreshOneOffAisUserWhoseUserSiteHasBeenFetchedBefore_userSiteShouldNotBeRefreshed() {
        var activeUserSite = createUserSite(yoltProviderSite.getId(), Instant.now().minusSeconds(60));

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());

        subject.refreshForUser(userId, true, true);

        verifyNoMoreInteractions(userSiteService, clientTokenRequesterService, userSiteRefreshService, properties);
    }


    @Test
    void refreshForUser_blacklistedProvider_userSiteShouldNotBeRefreshed() {
        var activeUserSite = createUserSite(yoltProviderSite.getId(), null);

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getBlacklistedProviders()).thenReturn(singletonList(activeUserSite.getProvider()));

        subject.refreshForUser(userId, false, false);

        verifyNoMoreInteractions(userSiteService, clientTokenRequesterService, userSiteRefreshService, properties);
    }

    @Test
    void refreshForUser_userSiteWasRecentlyRefreshed_userSiteShouldNotBeRefreshed() {
        var activeUserSite = createUserSite(yoltProviderSite.getId(), Instant.now(systemUTC()));

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getMinimumSecondsSinceLastRefresh()).thenReturn(10);
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());

        subject.refreshForUser(userId, false, false);

        verifyNoMoreInteractions(userSiteService, clientTokenRequesterService, userSiteRefreshService, properties);
    }

    @Test
    void refreshForUser_userSiteIsMigrating_userSiteShouldNotBeRefreshed() {
        var activeUserSite = createUserSite(yoltProviderSite.getId(), null)
                .toBuilder()
                .migrationStatus(MigrationStatus.MIGRATING_FROM)
                .build();

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(activeUserSite));
        when(properties.getMinimumSecondsSinceLastRefresh()).thenReturn(10);
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());

        subject.refreshForUser(userId, false, false);

        verifyNoMoreInteractions(userSiteService, clientTokenRequesterService, userSiteRefreshService, properties);
    }

    @Test
    void refreshForUser_userSiteInUnrecoverableState_userSiteShouldNotBeRefreshed() {
        var userSite = createUserSite(yoltProviderSite.getId(), ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null);

        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(singletonList(userSite));
        when(properties.getBlacklistedProviders()).thenReturn(emptyList());

        subject.refreshForUser(userId, false, false);

        verifyNoMoreInteractions(userSiteService, clientTokenRequesterService, userSiteRefreshService, properties);
    }


    private PostgresUserSite createUserSite(UUID siteId, ConnectionStatus connectionStatus, FailureReason failureReason, Instant lastDataFetch) {
        final PostgresUserSite userSite = new PostgresUserSite();
        userSite.setUserId(userId);
        userSite.setUserSiteId(userSiteId);
        userSite.setProvider("YOLT_PROVIDER");
        userSite.setSiteId(siteId);
        userSite.setExternalId("externalId");
        userSite.setConnectionStatus(connectionStatus);
        userSite.setFailureReason(failureReason);
        userSite.setLastDataFetch(lastDataFetch);
        userSite.setMigrationStatus(MigrationStatus.NONE);
        userSite.setClientId(new ClientId(clientId));
        return userSite;
    }

    private PostgresUserSite createUserSite(UUID siteId, Instant lastDataFetch) {
        return createUserSite(siteId, ConnectionStatus.CONNECTED, null, lastDataFetch);
    }
}
