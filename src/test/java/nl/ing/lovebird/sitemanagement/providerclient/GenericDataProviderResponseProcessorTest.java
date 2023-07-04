package nl.ing.lovebird.sitemanagement.providerclient;

import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatus;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.CustomExpiredConsentFlowService;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.providerresponse.GenericDataProviderResponseProcessor;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.USER_REFRESH;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class GenericDataProviderResponseProcessorTest {

    private static final UserSiteActionType USER_SITE_ACTION_TYPE = USER_REFRESH;

    @Mock
    private ActivityService activityService;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private SiteManagementMetrics siteManagementMetrics;
    @Mock
    private CustomExpiredConsentFlowService customExpiredConsentFlowService;

    private GenericDataProviderResponseProcessor responseProcessor;

    private final UUID clientId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID userSiteId = UUID.randomUUID();
    private final UUID siteId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();
    private PostgresUserSite userSite;
    private ClientUserToken clientUserToken;

    @BeforeEach
    void setUp() {
        responseProcessor = new GenericDataProviderResponseProcessor(activityService, userSiteService, siteManagementMetrics, customExpiredConsentFlowService);

        userSite = new PostgresUserSite();
        userSite.setUserId(userId);
        userSite.setUserSiteId(userSiteId);
        userSite.setSiteId(siteId);
        userSite.setProvider("YODLEE");
        JwtClaims clientClaims = TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), clientId);
        clientUserToken = new ClientUserToken("mocked-client-token-" + clientId, clientClaims);
    }


    @Test
    void testRefreshSuccessful() {

        // prepare inputs

        // prepare outputs

        // define behavior

        // nike: just do it!
        responseProcessor.process(userSiteId, Optional.ofNullable(userSite),
                ProviderServiceResponseStatus.FINISHED, UserSiteActionType.USER_REFRESH, activityId, clientUserToken);

        // check it
        verify(userSiteService).unlock(userSite);
        verify(siteManagementMetrics).incrementCounterFetchDataFinishSuccess(UserSiteActionType.USER_REFRESH, userSite);
    }

    @Test
    void testTokenInvalidFlowWhereUserSiteShouldDisconnect() {

        // prepare inputs

        // prepare outputs

        // define behavior
        when(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).thenReturn(true);

        // nike: just do it!
        responseProcessor.process(userSiteId, Optional.ofNullable(userSite),
                ProviderServiceResponseStatus.TOKEN_INVALID, UserSiteActionType.USER_REFRESH, activityId, clientUserToken);

        // check it
        verify(userSiteService).unlock(userSite);
        verify(siteManagementMetrics).incrementCounterFetchDataFinish(UserSiteActionType.USER_REFRESH, userSite, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS);
        verify(userSiteService).updateUserSiteStatus(eq(this.userSite), eq(ConnectionStatus.DISCONNECTED), eq(FailureReason.CONSENT_EXPIRED), isNull());
    }

    @Test
    void testTokenInvalidFlowWhereUserSiteShouldNotDisconnect() {

        // prepare inputs

        // prepare outputs

        // define behavior
        when(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).thenReturn(false);

        // nike: just do it!
        responseProcessor.process(userSiteId, Optional.ofNullable(userSite),
                ProviderServiceResponseStatus.TOKEN_INVALID, UserSiteActionType.USER_REFRESH, activityId, clientUserToken);

        // check it
        verify(userSiteService).unlock(userSite);
        verify(siteManagementMetrics).incrementCounterFetchDataFinish(UserSiteActionType.USER_REFRESH, userSite, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS);
        verify(userSiteService).updateUserSiteStatus(eq(this.userSite), eq(ConnectionStatus.CONNECTED), eq(FailureReason.CONSENT_EXPIRED), isNull());
    }

    @Test
    void testUnknownError() {

        // prepare inputs

        // prepare outputs

        // define behavior

        // nike: just do it!
        responseProcessor.process(userSiteId, Optional.of(userSite),
                ProviderServiceResponseStatus.UNKNOWN_ERROR, UserSiteActionType.USER_REFRESH, activityId, clientUserToken);

        // check it
        verify(userSiteService).unlock(userSite);
        verify(siteManagementMetrics).incrementCounterFetchDataFinish(UserSiteActionType.USER_REFRESH, userSite, RefreshedUserSiteEvent.Status.FAILED);
        verify(userSiteService).updateUserSiteStatus(eq(this.userSite), eq(ConnectionStatus.CONNECTED), eq(FailureReason.TECHNICAL_ERROR), isNull());
    }

    @Test
    void processNoSupportedAccounts() {
        when(userSiteService.getUserSite(userId, userSiteId)).thenReturn(userSite);

        responseProcessor.processNoSupportedAccountsMessage(userId, userSiteId, USER_SITE_ACTION_TYPE);

        verify(userSiteService, never()).updateUserSiteStatus(eq(userSite), any(), any(), any());
        verify(siteManagementMetrics).incrementCounterFetchDataFinishSuccess(USER_SITE_ACTION_TYPE, userSite);
        verify(userSiteService).unlock(userSite);
    }

    @Test
    void testBackPressureRequestSuccess() {

        // prepare inputs

        // prepare outputs

        // define behavior

        // nike: just do it!
        responseProcessor.process(userSiteId, Optional.ofNullable(userSite),
                ProviderServiceResponseStatus.BACK_PRESSURE_REQUEST, UserSiteActionType.USER_REFRESH, activityId, clientUserToken);

        // check it
        verify(userSiteService).unlock(userSite);
        verify(siteManagementMetrics).incrementCounterFetchDataFinish(UserSiteActionType.USER_REFRESH, userSite, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS);
        verify(userSiteService).updateUserSiteStatus(eq(this.userSite), eq(ConnectionStatus.CONNECTED), isNull(), isNull());
    }
}
