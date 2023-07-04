package nl.ing.lovebird.sitemanagement.usersitedelete;

import nl.ing.lovebird.activityevents.events.DeleteUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.exception.UserSiteDeleteException;
import nl.ing.lovebird.sitemanagement.exception.UserSiteIsNotMarkedAsDeletedException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static java.time.Clock.systemUTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class UserSiteDeleteServiceTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final UUID USER_ID = randomUUID();
    private static final UUID SITE_ID = randomUUID();
    private static final UUID USER_SITE_ID = randomUUID();
    private static final String EXTERNAL_ID = "325234";
    private static final String PROVIDER = "SALTEDGE";
    @Captor
    private ArgumentCaptor<DeleteUserSiteEvent> deleteUserSiteEventArgumentCaptor = ArgumentCaptor.forClass(DeleteUserSiteEvent.class);

    @Mock
    private PostgresUserSiteRepository postgresUserSiteRepository;
    @Mock
    private MaintenanceClient maintenanceClient;
    @Mock
    private ActivityService activityService;
    @Mock
    private UserSiteDeleteProviderService userSiteDeleteProviderService;
    @Mock
    private UserSiteEventService userSiteEventService;
    @Mock
    private ConsentSessionService userSiteSessionService;
    @Mock
    private ClientTokenRequesterService clientUserTokenRequesterService;
    @Mock
    private AccessMeansManager accessMeansManager;
    @Mock
    private UserSiteService userSiteService;

    @InjectMocks
    private UserSiteDeleteService userSiteDeleteService;

    private ClientUserToken clientUserToken;

    @BeforeEach
    void setUp() {
        userSiteDeleteService = new UserSiteDeleteService(systemUTC(),
                postgresUserSiteRepository,
                maintenanceClient,
                activityService,
                userSiteDeleteProviderService,
                userSiteEventService,
                null,
                userSiteSessionService,
                clientUserTokenRequesterService,
                accessMeansManager,
                userSiteService);

        JwtClaims clientUserClaims = TestJwtClaims.createClientUserClaims("junit", randomUUID(), CLIENT_ID.unwrap(), USER_ID);
        clientUserToken = new ClientUserToken("mocked-client-user-token-" + USER_ID, clientUserClaims);

        when(clientUserTokenRequesterService.getClientUserToken(CLIENT_ID.unwrap(), USER_ID)).thenReturn(clientUserToken);
    }

    @Test
    void testDeleteUserSiteFormProvider() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setDeleted(true);
        final List<PostgresUserSite> userSites = emptyList();
        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(userSites);
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        verify(postgresUserSiteRepository).deleteUserSite(userSite.getUserSiteId());
        verify(accessMeansManager).deleteAccessMeansForUserSite(any(), any(), anyBoolean());
        verify(userSiteService).unlock(userSite);
        verify(userSiteDeleteProviderService).deleteUserSiteBlocking(userSite, emptyList(), null, clientUserToken);
        // make sure we never call it asynchronously for the maintenance delete call as maintenance should try again when it fails.
        verify(userSiteDeleteProviderService, never()).deleteUserSiteAsync(eq(userSite), anyList(), any(), any());
    }

    @Test
    void testDeleteUserSiteUrlProvider() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "STARLINGBANK", null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setDeleted(true);
        final List<PostgresUserSite> userSites = emptyList();
        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(userSites);
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        verify(postgresUserSiteRepository).deleteUserSite(userSite.getUserSiteId());
        verify(accessMeansManager).deleteAccessMeansForUserSite(any(), any(), anyBoolean());
        verify(userSiteService).unlock(userSite);
        verify(userSiteDeleteProviderService).deleteUserSiteBlocking(userSite, emptyList(), null, clientUserToken);
        // make sure we never call it asynchronously for the maintenance delete call as maintenance should try again when it fails.
        verify(userSiteDeleteProviderService, never()).deleteUserSiteAsync(eq(userSite), anyList(), any(), any());
    }

    @Test
    void testDeleteUserSite_moreThan1UserSiteAtProviderScrapingConnections() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setDeleted(true);
        final PostgresUserSite userSite2 = new PostgresUserSite(USER_ID, randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite2.setExternalId("same site, same provider");

        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(List.of(userSite, userSite2));
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(USER_ID)).thenReturn(List.of(userSite, userSite2));

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        verify(postgresUserSiteRepository).deleteUserSite(userSite.getUserSiteId());
        verify(userSiteDeleteProviderService).deleteUserSiteBlocking(userSite, singletonList(userSite2), null, clientUserToken);

        verify(accessMeansManager).deleteAccessMeansForUserSite(any(), any(), eq(true));
    }

    @Test
    void testDeleteUserSite_moreThan1UserSiteAtProviderForNonScrapingConnections() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "YOLT_PROVIDER", null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setDeleted(true);
        final PostgresUserSite userSite2 = new PostgresUserSite(USER_ID, randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "YOLT_PROVIDER", null, null, null, false, null);
        userSite2.setExternalId("same site, same provider");

        final List<PostgresUserSite> userSites = List.of(userSite, userSite2);
        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(userSites);
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        // Check specifically that the access means is deleted (rather than kept as testcase above) because it's stored per usersite.
        verify(accessMeansManager).deleteAccessMeansForUserSite(any(), any(), anyBoolean());
    }

    @Test
    void testDeleteUserSite_moreThan1UserSiteAtProvider_SecondSiteAlreadyDeleted() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setDeleted(true);

        final PostgresUserSite userSite2 = new PostgresUserSite(USER_ID, randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite2.setExternalId("same site, same provider");

        final List<PostgresUserSite> userSites = Collections.singletonList(userSite);
        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(userSites);
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        verify(postgresUserSiteRepository).deleteUserSite(userSite.getUserSiteId());
        verify(accessMeansManager).deleteAccessMeansForUserSite(any(), any(), eq(false));

        verify(userSiteDeleteProviderService).deleteUserSiteBlocking(userSite, Collections.emptyList(), null, clientUserToken);
    }

    @Test
    void testDeleteUserSite_whenNotMarkedAsDelete() {
        assertThatThrownBy(() -> {
            when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.ofNullable(getUserSite(USER_SITE_ID).toBuilder()
                    .userId(USER_ID)
                    .isDeleted(false)
                    .build()));
            userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);
        }).isInstanceOf(UserSiteIsNotMarkedAsDeletedException.class);
    }

    @Test
    void testDeleteUserSite_willNotBeRetriedWhenAlreadyDeleted() {
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.empty());

        userSiteDeleteService.deleteUserSite(USER_ID, USER_SITE_ID, null);

        verify(userSiteDeleteProviderService, never()).deleteUserSiteBlocking(any(PostgresUserSite.class), anyList(), any(), any());
        verify(postgresUserSiteRepository, times(0)).deleteUserSite(any());
        verify(accessMeansManager, never()).deleteAccessMeansForUserSite(any(), any(), anyBoolean());
        verify(userSiteService, never()).unlock(any());
    }

    @Test
    void testDeleteUserSite_errorSchedulingDelete() {
        assertThatThrownBy(() -> {
            final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
            userSite.setExternalId(EXTERNAL_ID);
            when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

            doThrow(RestClientException.class).when(maintenanceClient).scheduleUserSiteDelete(userSite.getUserId(), userSite.getUserSiteId());

            userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(USER_SITE_ID, null, clientUserToken);

            verify(userSiteDeleteProviderService, never()).deleteUserSiteAsync(eq(userSite), anyList(), any(), any());
        }).isInstanceOf(UserSiteDeleteException.class);
    }

    @Test
    void testMarkForDelete() {
        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        userSite.setExternalId(EXTERNAL_ID);
        userSite.setProvider(PROVIDER);
        when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(emptyList());
        when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));

        userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(USER_SITE_ID, null, clientUserToken);

        verify(userSiteService).markAsDeleted(USER_ID, USER_SITE_ID);
        verify(activityService).startActivity(any(ClientUserToken.class), deleteUserSiteEventArgumentCaptor.capture());
        assertThat(deleteUserSiteEventArgumentCaptor.getValue().getUserSiteId()).isEqualTo(USER_SITE_ID);
        verify(maintenanceClient).scheduleUserSiteDelete(USER_ID, USER_SITE_ID);
        verify(userSiteDeleteProviderService).deleteUserSiteAsync(eq(userSite), eq(emptyList()), any(), any());
        verify(userSiteEventService).publishDeleteUserSiteEvent(any(PostgresUserSite.class), any(ClientToken.class));
    }

    @Test
    void testMarkForDelete_shouldDoNothingIfAlreadyDeleted() {
        final PostgresUserSite unknownUserSite = new PostgresUserSite(USER_ID, randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
        final PostgresUserSite alreadyMarkedUserSite = new PostgresUserSite(USER_ID, randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);

        when(postgresUserSiteRepository.getUserSite(USER_ID, unknownUserSite.getUserSiteId())).thenReturn(Optional.empty());

        userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(unknownUserSite.getUserSiteId(), null, clientUserToken);
        userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(alreadyMarkedUserSite.getUserSiteId(), null, clientUserToken);

        verify(userSiteService, never()).markAsDeleted(eq(USER_ID), any());
        verify(activityService, never()).startActivity(any(ClientUserToken.class), any());
        verify(maintenanceClient, never()).scheduleUserSiteDelete(any(), any());
        verify(userSiteDeleteProviderService, never()).deleteUserSiteAsync(any(), any(), any(), any());
        verify(userSiteEventService, never()).publishDeleteUserSiteEvent(any(PostgresUserSite.class), any(ClientToken.class));
    }

    @Test
    void testMarkForDelete_shouldStillMarkForDeleteWhenExternalDeleteFails() {
        assertThatThrownBy(() -> {
            final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, PROVIDER, null, null, null, false, null);
            userSite.setExternalId(EXTERNAL_ID);
            when(postgresUserSiteRepository.getUserSites(USER_ID)).thenReturn(emptyList());
            when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.of(userSite));
            doThrow(IllegalThreadStateException.class).when(userSiteDeleteProviderService).deleteUserSiteAsync(eq(userSite), eq(emptyList()), any(), any());

            try {
                userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(USER_SITE_ID, null, clientUserToken);
            } finally {
                verify(maintenanceClient).scheduleUserSiteDelete(USER_ID, USER_SITE_ID);
            }
        }).isInstanceOf(IllegalThreadStateException.class);
    }

    private static PostgresUserSite getUserSite(final UUID userSiteId) {
        return new PostgresUserSite(UserSiteDeleteServiceTest.USER_ID, userSiteId, UserSiteDeleteServiceTest.SITE_ID, "external_id",
                ConnectionStatus.DISCONNECTED, null, null, Instant.now(Clock.systemUTC()),
                null, null, ClientId.random(), "YODLEE", null, null, null, false, null);
    }
}
