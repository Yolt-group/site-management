package nl.ing.lovebird.sitemanagement.usersite;

import com.yolt.securityutils.crypto.SecretKey;
import lombok.SneakyThrows;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.RefreshUserSitesEvent;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.StartEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.accessmeans.CustomExpiredConsentFlowService;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansHolder;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.accessmeans.UserSiteAccessMeans;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.AccountsAndTransactionsClient;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.ApiFetchDataDTO;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.time.Clock.systemUTC;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager.AccessMeansResult.ResultCode.DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED;
import static nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager.AccessMeansResult.ResultCode.UNKNOWN_ERROR;
import static nl.ing.lovebird.sitemanagement.accessmeans.AesEncryptionUtil.encrypt;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.CREATE_USER_SITE;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.USER_REFRESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class UserSiteRefreshServiceTest {

    static final SecretKey KEY = SecretKey.from("a3f60fafc948035382fbe9ce7b4535c4".getBytes());
    // <editor-fold desc="@Mock all the dependencies of UserSiteRefreshService." defaultstate="collapsed">
    @Mock
    ProviderRequestRepository providerRequestRepository;
    @Mock
    FormProviderRestClient formProviderRestClient;
    @Mock
    AccessMeansManager accessMeansManager;
    @Mock
    CustomExpiredConsentFlowService customExpiredConsentFlowService;
    @Mock
    UserService userService;
    @Mock
    UserSiteService userSiteService;
    @Mock(answer = Answers.RETURNS_MOCKS)
    SiteService siteService;
    @Mock
    ActivityService activityService;
    @Mock
    SiteManagementMetrics siteManagementMetrics;
    @Mock
    ProviderRestClient providerRestClient;
    @Mock
    AuthenticationMeansFactory authenticationMeansFactory;
    @Mock
    AccountsAndTransactionsClient accountsAndTransactionsClient;
    @Mock
    ClientSiteService clientSiteService;
    // </editor-fold>

    // <editor-fold desc="Static test data." defaultstate="collapsed">
    @Mock
    ClientUserToken clientUserToken;
    // </editor-fold>

    UserSiteRefreshService subject;

    @BeforeEach
    void setUp() {
        var clock = systemUTC();
        subject = new UserSiteRefreshService(clock,
                providerRequestRepository,
                formProviderRestClient,
                accessMeansManager,
                customExpiredConsentFlowService,
                userService,
                userSiteService,
                siteService,
                activityService,
                siteManagementMetrics,
                providerRestClient,
                authenticationMeansFactory,
                new LastFetchedService(clock),
                clientSiteService,
                accountsAndTransactionsClient
        );

        when(clientUserToken.getClientIdClaim()).thenReturn(UUID.randomUUID());
        when(siteManagementMetrics.timeUserSitesRefresh(any()))
                .then(invocation -> ((Callable<?>) invocation.getArguments()[0]).call());
    }

    // <editor-fold desc="Happy flow." defaultstate="collapsed">

    @Test
    @SneakyThrows
    void given_UserSite_when_Refresh_then_UserSiteIsRefreshed() {
        // given a user site with valid access means that can be locked
        PostgresUserSite userSite = createUserSite();
        when(accessMeansManager.retrieveValidAccessMeans(any(), any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(
                        new UserSiteAccessMeans(UUID.randomUUID(), UUID.randomUUID(), "YOLT_PROVIDER", encrypt("hoi", KEY), new java.util.Date(), new java.util.Date(), Instant.EPOCH),
                        KEY
                )));
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        when(authenticationMeansFactory.createAuthMeans(clientUserToken, userSite.getRedirectUrlId())).thenReturn(new AuthenticationMeansReference(userSite.getClientId().unwrap(), userSite.getRedirectUrlId()));

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        // then a data fetch is triggered ...
        verify(providerRestClient).fetchData(any(), any(), any(), any(), any(boolean.class));
        // .. and an appropriate UserSiteStartEvent is sent
        verify(activityService).startActivity(eq(clientUserToken), any(RefreshUserSitesEvent.class));
        // .. and the user site is locked
        verify(userSiteService).attemptLock(any(), any());
        // .. and the status is set to INITIAL_PROCESSING
        verify(userSiteService).updateUserSiteStatus(any(), eq(ConnectionStatus.CONNECTED), isNull(), any());
    }

    @Test
    @SneakyThrows
    void given_MultipleUserSites_when_Refresh_then_AllUserSitesAreRefreshed() {
        // given two userssites with valid access means that can both be locked
        PostgresUserSite userSiteA = createUserSite();
        PostgresUserSite userSiteB = createUserSite();
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        when(accessMeansManager.retrieveValidAccessMeans(any(), any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(
                        new UserSiteAccessMeans(UUID.randomUUID(), UUID.randomUUID(), "YOLT_PROVIDER", encrypt("hoi", KEY), new java.util.Date(), new java.util.Date(), Instant.EPOCH),
                        KEY
                )));
        when(authenticationMeansFactory.createAuthMeans(eq(clientUserToken), any())).thenReturn(new AuthenticationMeansReference(UUID.randomUUID(), UUID.randomUUID()));

        // when refreshing
        subject.refreshUserSitesBlocking(asList(userSiteA, userSiteB), false, clientUserToken, USER_REFRESH, null, null);

        // then a data fetch is triggered twice ...
        verify(providerRestClient, times(2)).fetchData(any(), any(), any(), any(), any(boolean.class));
        // .. and an appropriate UserSiteStartEvent is sent
        verify(activityService).startActivity(eq(clientUserToken), any(RefreshUserSitesEvent.class));
        // .. and the user site is locked
        verify(userSiteService, times(2)).attemptLock(any(), any());
        // .. and the status is set to INITIAL_PROCESSING
        verify(userSiteService, times(2)).updateUserSiteStatus(any(), eq(ConnectionStatus.CONNECTED), isNull(), any());
    }

    // </editor-fold>

    // <editor-fold desc="Edge cases." defaultstate="collapsed">

    @Test
    void given_EmptyCollection_when_Refresh_then_NothingShouldHappen() {
        // when refreshing nothing
        subject.refreshUserSitesBlocking(emptyList(), false, clientUserToken, USER_REFRESH, null, null);

        verify(siteManagementMetrics).timeUserSitesRefresh(any());

        // then ...
        verifyNoMoreInteractions(
                // ... no other metrics are changed
                siteManagementMetrics
        );
        verifyNoInteractions(
                // ... no provider requests are made
                providerRequestRepository,
                // ... no data fetches are triggered
                providerRestClient,
                formProviderRestClient,
                // ... no access means are retrieved
                accessMeansManager,
                // ... no events are sent
                activityService
        );
    }

    @Test
    void given_ScrapingUserSiteThatWasJustCreated_when_Refresh_then_IllegalArgumentExceptionIsThrown() {
        assertThatThrownBy(() -> {
            // given a user site with valid access means, that can be locked, and a scraping provider (Yodlee in this case)
            PostgresUserSite scrapingUserSite = createUserSite();
            scrapingUserSite.setProvider("YODLEE");

            // when refreshing
            subject.refreshUserSitesBlocking(singleton(scrapingUserSite), false, clientUserToken, CREATE_USER_SITE, null, null);

            // then an IllegalArgumentException is thrown.
        }).isInstanceOf(IllegalArgumentException.class);
    }

    // </editor-fold>

    // <editor-fold desc="Testing the logic that determines if a PostgresUserSite should be refreshed or not.">

    @Test
    @SneakyThrows
    void given_UserSiteIneligibleForRefresh_when_Refresh_then_UserSiteIsNotRefreshed() {
        // given a user site ...
        PostgresUserSite userSite = createUserSite();
        // ... that requires some form of user interaction
        userSite.setConnectionStatus(ConnectionStatus.STEP_NEEDED);

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        verify(siteManagementMetrics).timeUserSitesRefresh(any());

        // then ...
        verifyNoMoreInteractions(
                // ... no other metrics are changed
                siteManagementMetrics
        );
        verifyNoInteractions(
                // ... no provider requests are made
                providerRequestRepository,
                // ... no data fetches are triggered
                providerRestClient,
                formProviderRestClient,
                // ... no access means are retrieved
                accessMeansManager,
                // ... no events are sent
                activityService
        );
    }

    @Test
    @SneakyThrows
    void given_LockedUserSite_when_Refresh_then_UserSiteIsNotRefreshed() {
        // given a user site ...
        PostgresUserSite userSite = createUserSite();
        // .. that cannot be locked (because it is already locked)
        when(userSiteService.attemptLock(any(), any())).thenReturn(false);
        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        verify(siteManagementMetrics).timeUserSitesRefresh(any());

        // then ...
        verifyNoMoreInteractions(
                // ... no other metrics are changed
                siteManagementMetrics
        );
        verifyNoInteractions(
                // ... no provider requests are made
                providerRequestRepository,
                // ... no data fetches are triggered
                providerRestClient,
                formProviderRestClient,
                // ... no access means are retrieved
                accessMeansManager,
                // ... no events are sent
                activityService
        );
    }

    // </editor-fold>

    // <editor-fold desc="Testing what happens when the retrieval of AccessMeans returns errors.">

    @Test
    @SneakyThrows
    void given_ErrorDuringAccessMeansRetrieval_when_Refresh_then_UserSiteIsNotRefreshed() {
        // given a user site for which the retrievel of access means failed, but that can be locked
        PostgresUserSite userSite = createUserSite();
        when(accessMeansManager.retrieveValidAccessMeans(any(),  any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(UNKNOWN_ERROR));
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        // then no data is fetched ...
        verify(providerRestClient, never()).fetchData(any(), any(), any(), any(), any(boolean.class));
        // ... and the status transitions to TECHNICAL ERROR
        verify(userSiteService).updateUserSiteStatus(any(), eq(ConnectionStatus.CONNECTED), eq(FailureReason.TECHNICAL_ERROR), any());
    }

    @Test
    @SneakyThrows
    void given_ConsentExpiredDuringAccessMeansRetrievalAndShouldDisconnectUserSite_when_Refresh_then_UserSiteIsNotRefreshedAndStatusBecomesDisconnected() {
        // given a user site for which the consent has expired, but that can be locked
        PostgresUserSite userSite = createUserSite();
        when(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).thenReturn(true);
        when(accessMeansManager.retrieveValidAccessMeans(any(), any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED));
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite),  false, clientUserToken, USER_REFRESH, null, null);

        // then no data is fetched ...
        verify(providerRestClient, never()).fetchData(any(), any(), any(), any(), any(boolean.class));
        // ... and the status transitions to AUTHENTICATION_FAILED
        verify(userSiteService).updateUserSiteStatus(any(), eq(ConnectionStatus.DISCONNECTED), eq(FailureReason.AUTHENTICATION_FAILED), any());
    }

    @Test
    @SneakyThrows
    void given_ConsentExpiredDuringAccessMeansRetrievalAndShouldNotDisconnectUserSite_when_Refresh_then_UserSiteIsNotRefreshedAndStatusRemainsConnected() {
        // given a user site for which the consent has expired, but that can be locked
        PostgresUserSite userSite = createUserSite();
        when(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).thenReturn(false);
        when(accessMeansManager.retrieveValidAccessMeans(any(), any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED));
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite),  false, clientUserToken, USER_REFRESH, null, null);

        // then no data is fetched ...
        verify(providerRestClient, never()).fetchData(any(), any(), any(), any(), any(boolean.class));
        // ... and the status transitions to AUTHENTICATION_FAILED
        verify(userSiteService).updateUserSiteStatus(any(), eq(ConnectionStatus.CONNECTED), eq(FailureReason.AUTHENTICATION_FAILED), any());
    }

    // </editor-fold>

    // <editor-fold desc="Testing what happens when an error occurs during the trigger data fetch call.">

    @Test
    @SneakyThrows
    void given_ErrorDuringTriggerDataFetch_when_Refresh_then_UserSiteIsNotRefreshed() {
        // given a user site with valid access means, that can be locked ...
        PostgresUserSite userSite = createUserSite();
        when(accessMeansManager.retrieveValidAccessMeans(any(), any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(
                        new UserSiteAccessMeans(UUID.randomUUID(), UUID.randomUUID(), "YOLT_PROVIDER", "hoi", new java.util.Date(), new java.util.Date(), Instant.EPOCH),
                        mock(SecretKey.class)
                )));
        when(clientUserToken.getUserIdClaim()).thenReturn(userSite.getUserId());
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        when(authenticationMeansFactory.createAuthMeans(eq(clientUserToken), any())).thenReturn(new AuthenticationMeansReference(UUID.randomUUID(), UUID.randomUUID()));

        // ... but for which fetching data fails
        doThrow(new HttpException(500, null)).when(providerRestClient).fetchData(any(),  any(), any(), any(), any(boolean.class));

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite),  false, clientUserToken, USER_REFRESH, null, null);

        // then the error is handled gracefully ...
        // ... status is set to REFRESH_FAILED
        verify(userSiteService).updateUserSiteStatus(eq(userSite), eq(ConnectionStatus.CONNECTED), eq(FailureReason.TECHNICAL_ERROR), any());
        // ... the user site is unlocked
        verify(userSiteService).unlock(userSite);
        // ... two activityEvents are sent
        var startEventCaptor = ArgumentCaptor.forClass(StartEvent.class);
        verify(activityService).startActivity(eq(clientUserToken), startEventCaptor.capture());

        assertThat(startEventCaptor.getValue().getType()).isEqualTo(EventType.REFRESH_USER_SITES);
        assertThat(startEventCaptor.getValue().getUserId()).isEqualTo(userSite.getUserId());

        verify(activityService).handleFailedRefresh(eq(clientUserToken), any(), eq(userSite), eq(RefreshedUserSiteEvent.Status.FAILED));
    }

    // </editor-fold>

    // <editor-fold desc="Testing the catch clause, making sure that all UserSites will be unlocked, and that appropriate events are sent.">

    @Test
    @SneakyThrows
    void given_UnexpectedExceptionWhenSendingUserSiteStartEvent_when_Refresh_then_ErrorIsHandledGracefully() {
        // given a user site that can be locked ...
        PostgresUserSite userSite = createUserSite();
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        // ... but for which sending the UserSiteStartEvent fails because of a technical error
        doThrow(new RuntimeException()).when(activityService).startActivity(eq(clientUserToken), any(RefreshUserSitesEvent.class));

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        // then the error is handled gracefully ...
        // ... status is set to UNKNOWN
        verify(userSiteService).updateUserSiteStatus(eq(userSite), eq(ConnectionStatus.CONNECTED), eq(FailureReason.TECHNICAL_ERROR), any());
        // ... the user site is unlocked
        verify(userSiteService).unlock(userSite);
        // ... an activityEvent is **not** sent
        verify(activityService, never()).handleFailedRefresh(eq(clientUserToken), any(), any(), any());
    }

    @Test
    @SneakyThrows
    void given_UnexpectedExceptionAfterSendingUserSiteStartEvent_when_Refresh_then_ErrorIsHandledGracefully() {
        // given a user site that can be locked ...
        PostgresUserSite userSite = createUserSite();
        when(clientUserToken.getUserIdClaim()).thenReturn(userSite.getUserId());
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        // ... but for which updating the status to INITIAL_PROCESSING fails because of a technical error
        doThrow(new RuntimeException()).when(userSiteService).updateUserSiteStatus(eq(userSite), eq(ConnectionStatus.CONNECTED), isNull(), any());

        // when refreshing
        subject.refreshUserSitesBlocking(singleton(userSite), false, clientUserToken, USER_REFRESH, null, null);

        // then the error is handled gracefully ...
        // ... status is set to UNKNOWN
        verify(userSiteService).updateUserSiteStatus(eq(userSite), eq(ConnectionStatus.CONNECTED), eq(FailureReason.TECHNICAL_ERROR), any());
        // ... the user site is unlocked
        verify(userSiteService).unlock(userSite);
        // ... two activityEvent are sent
        var startEventCaptor = ArgumentCaptor.forClass(StartEvent.class);

        verify(activityService).startActivity(eq(clientUserToken), startEventCaptor.capture());
        assertThat(startEventCaptor.getValue().getType()).isEqualTo(EventType.REFRESH_USER_SITES);
        assertThat(startEventCaptor.getValue().getUserId()).isEqualTo(userSite.getUserId());

        verify(activityService).handleFailedRefresh(eq(clientUserToken), any(), eq(userSite), eq(RefreshedUserSiteEvent.Status.FAILED));
    }

    // </editor-fold>

    // <editor-fold desc="Testing logic related to one-off AIS users.">
    @Test
    @SneakyThrows
    void refreshUserSitesBlocking_oneOffAisUserWithFetchedUserSiteAndNonFetchedUserSite_onlyNonFetchedUserSiteIsRefreshed() {
        var fetchedUserSite = createUserSite();
        var nonFetchedUserSite = createUserSite().toBuilder().lastDataFetch(null).build();

        when(accessMeansManager.retrieveValidAccessMeans(any(),  any(), any(), any()))
                .thenReturn(new AccessMeansManager.AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(
                        new UserSiteAccessMeans(UUID.randomUUID(), UUID.randomUUID(), "YOLT_PROVIDER", encrypt("hi", KEY), new java.util.Date(), new java.util.Date(), Instant.EPOCH),
                        KEY
                )));
        when(userSiteService.attemptLock(any(), any())).thenReturn(true);
        when(authenticationMeansFactory.createAuthMeans(clientUserToken, nonFetchedUserSite.getRedirectUrlId())).thenReturn(new AuthenticationMeansReference(nonFetchedUserSite.getClientId().unwrap(), nonFetchedUserSite.getRedirectUrlId()));

        // when refreshing
        subject.refreshUserSitesBlocking(List.of(fetchedUserSite, nonFetchedUserSite), true, clientUserToken, USER_REFRESH, null, null);

        // then a data fetch is triggered for the non-fetched user site...
        var fetchCaptor = ArgumentCaptor.forClass(ApiFetchDataDTO.class);
        verify(providerRestClient).fetchData(any(), any(), fetchCaptor.capture(), any(), any(boolean.class));
        assertThat(fetchCaptor.getValue().getUserSiteId()).isEqualTo(nonFetchedUserSite.getUserSiteId());

        // .. and an appropriate UserSiteStartEvent is sent
        var refreshEventCaptor = ArgumentCaptor.forClass(RefreshUserSitesEvent.class);
        verify(activityService).startActivity(eq(clientUserToken), refreshEventCaptor.capture());
        assertThat(refreshEventCaptor.getValue().getUserSiteIds()).containsOnly(nonFetchedUserSite.getUserSiteId());

        // .. and the user site is locked
        verify(userSiteService).attemptLock(eq(nonFetchedUserSite), any());

        // .. and the status is set to INITIAL_PROCESSING
        verify(userSiteService).updateUserSiteStatus(eq(nonFetchedUserSite), eq(ConnectionStatus.CONNECTED), isNull(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @SneakyThrows
    void refreshUserSitesBlocking_noOneOffAisDataProvided_fetchesUserAndDelegatesCall(boolean oneOffAisUser) {
        var userId = UUID.randomUUID();
        var userSite = createUserSite();
        var subjectSpy = spy(subject);

        var expectedResult = Optional.of(UUID.randomUUID());

        when(clientUserToken.getUserIdClaim()).thenReturn(userId);
        when(userService.getUserOrThrow(userId)).thenReturn(
                new User(userId, null, new ClientId(UUID.randomUUID()), StatusType.ACTIVE, oneOffAisUser));
        doReturn(expectedResult).when(subjectSpy).refreshUserSitesBlocking(
                singleton(userSite), oneOffAisUser, clientUserToken, USER_REFRESH, null, null);

        var actualResult = subjectSpy.refreshUserSitesBlocking(singleton(userSite), clientUserToken, USER_REFRESH, null, null);

        assertThat(actualResult).isEqualTo(expectedResult);
    }
    // </editor-fold>


    // <editor-fold desc="Utility functions to generate mock data.">

    PostgresUserSite createUserSite() {
        return createUserSite("ING_NL");
    }

    PostgresUserSite createUserSite(String provider) {
        java.util.Date created = Date.from(Instant.EPOCH);
        java.util.Date updated = Date.from(Instant.EPOCH);
        java.util.Date lastDataFetch = Date.from(Instant.EPOCH);
        java.util.Date deletedAt = java.util.Date.from(Instant.now());
        return new PostgresUserSite(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, ConnectionStatus.CONNECTED, null, null, created.toInstant(), updated.toInstant(), lastDataFetch.toInstant(), ClientId.random(), provider, MigrationStatus.NONE, new UUID(0, 0), emptyMap(), false, deletedAt.toInstant());
    }

    // </editor-fold>

    @Test
    void testValidateRefreshRequest() {
        boolean result = subject.validRefreshUserSitesRequest(List.of(createUserSite()), CREATE_USER_SITE);
        assertThat(result).isTrue();
    }

    @Test
    void testValidateRefreshRequestWithoutSites() {
        boolean result = subject.validRefreshUserSitesRequest(List.of(), CREATE_USER_SITE);
        assertThat(result).isFalse();
    }

    @Test
    void testValidateRefreshRequestWithUnsupportedActionTypeForMultipleSites() {
        assertThatThrownBy(() -> {
            subject.validRefreshUserSitesRequest(List.of(createUserSite("ING_NL"), createUserSite()), CREATE_USER_SITE);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidateRefreshRequestForScrapingProvider() {
        assertThatThrownBy(() -> {
            subject.validRefreshUserSitesRequest(List.of(createUserSite("BUDGET_INSIGHT")), CREATE_USER_SITE);
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
