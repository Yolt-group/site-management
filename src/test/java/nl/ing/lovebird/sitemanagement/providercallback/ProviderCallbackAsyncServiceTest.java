package nl.ing.lovebird.sitemanagement.providercallback;

import lombok.SneakyThrows;
import nl.ing.lovebird.activityevents.events.RefreshUserSitesFlywheelEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.providerdomain.ProviderAccountDTO;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatus;
import nl.ing.lovebird.providershared.callback.UserDataCallbackResponse;
import nl.ing.lovebird.providershared.callback.UserSiteData;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.exception.CallbackIdentifierNotKnownException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.providerresponse.ScrapingDataProviderResponseProcessor;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static java.time.Clock.systemUTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class ProviderCallbackAsyncServiceTest {

    @Mock
    private UserSiteService userSiteService;
    @Mock
    private ScrapingDataProviderResponseProcessor scrapingDataProviderResponseProcessor;
    @Mock
    private UserExternalIdRepository userExternalIdRepository;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private SiteService siteService;
    @Mock
    private FormProviderRestClient formProviderRestClient;
    @Mock
    private UserService userService;
    @Mock
    private ProviderRequestRepository providerRequestRepository;
    @Mock
    private ClientTokenRequesterService callbacksClientTokenService;
    @Mock
    private ActivityService activityService;

    private ProviderCallbackAsyncService providerCallbackService;

    @Mock
    private SiteManagementMetrics siteManagementMetrics;

    private UUID userId;

    @BeforeEach
    void setUp() {
        Map<String, String> jsonPathConfig = new HashMap<>();
        jsonPathConfig.put("BUDGET_INSIGHT", "$.user.id");
        jsonPathConfig.put("SALTEDGE", "$.data.customer_id");
        providerCallbackService = new ProviderCallbackAsyncService(systemUTC(), userSiteService, scrapingDataProviderResponseProcessor,
                userExternalIdRepository, activityService, siteService, formProviderRestClient,
                providerRequestRepository, new CallbackConfiguration(jsonPathConfig),
                userService, callbacksClientTokenService, siteManagementMetrics);

        userId = randomUUID();
    }

    @Test
    void testCallback() throws CallbackIdentifierNotKnownException {
        final String extUserId = "extUserId";
        final String extUserSiteId = "extUserSiteId";
        final UUID internalUserSiteId = randomUUID();
        final UUID activityId = randomUUID();
        UUID siteId = randomUUID();
        final ClientId clientId = ClientId.random();

        PostgresUserSite userSite = new PostgresUserSite(userId, internalUserSiteId, siteId, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, clientId, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(extUserSiteId);

        ProviderAccountDTO providerAccountDTO = ProviderAccountDTO.builder().accountId("accountid").build();

        UserSiteData userSiteData = new UserSiteData(
                extUserSiteId,
                singletonList(providerAccountDTO),
                emptyList(),
                ProviderServiceResponseStatus.FINISHED);

        UserDataCallbackResponse userDataCallbackResponse = new UserDataCallbackResponse("BUDGET_INSIGHT", extUserId, userSiteData);

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", extUserId))
                .thenReturn(Optional.of(new UserExternalId(userId, "BUDGET_INSIGHT", extUserId)));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(userId)).thenReturn(singletonList(userSite));

        when(userSiteService.checkLock(userSite))
                .thenReturn(Optional.of(new PostgresUserSiteLock(internalUserSiteId, activityId, null)));
        when(providerRequestRepository.find(userId, activityId)).thenReturn(singletonList(new ProviderRequest(randomUUID(), activityId,
                userId, internalUserSiteId, UserSiteActionType.CREATE_USER_SITE)));
        var clientUserToken = new ClientUserToken("serializedCT", null);
        when(callbacksClientTokenService.getClientUserToken(clientId.unwrap(), userId)).thenReturn(clientUserToken);

        providerCallbackService.processCallback(userDataCallbackResponse);
        verify(userSiteService).getAllUserSitesIncludingDeletedOnes(userId);

        verify(scrapingDataProviderResponseProcessor).process(any(), eq(Optional.of(userSite)), eq(ProviderServiceResponseStatus.FINISHED), eq(UserSiteActionType.CREATE_USER_SITE), eq(activityId), eq(clientUserToken));
    }

    @Test
    void testCallbackNotInitiatedByUser() throws CallbackIdentifierNotKnownException { // Means no usersite lock.
        final String extUserId = "extUserId";
        final String extUserSiteId = "extUserSiteId";
        UUID siteId = randomUUID();
        final ClientId clientId = ClientId.random();

        PostgresUserSite userSite = new PostgresUserSite(this.userId, userId, siteId, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, clientId, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(extUserSiteId);

        ProviderAccountDTO providerAccountDTO = ProviderAccountDTO.builder()
                .accountId("accountid")
                .build();

        UserSiteData userSiteData = new UserSiteData(
                extUserSiteId,
                singletonList(providerAccountDTO),
                emptyList(),
                ProviderServiceResponseStatus.FINISHED);
        UserDataCallbackResponse userDataCallbackResponse = new UserDataCallbackResponse("BUDGET_INSIGHT", extUserId, userSiteData);

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", extUserId))
                .thenReturn(Optional.of(new UserExternalId(userId, "BUDGET_INSIGHT", extUserId)));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(this.userId)).thenReturn(singletonList(userSite));
        when(userSiteService.checkLock(userSite)).thenReturn(Optional.empty());
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("user-id", userId.toString());
        when(callbacksClientTokenService.getClientUserToken(clientId.unwrap(), userId)).thenReturn(new ClientUserToken("serializedCT", jwtClaims));

        providerCallbackService.processCallback(userDataCallbackResponse);

        var eventCaptor = ArgumentCaptor.forClass(RefreshUserSitesFlywheelEvent.class);
        verify(activityService).startActivity(any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserSiteIds().size()).isEqualTo(1);
        assertThat(eventCaptor.getValue().getUserSiteIds().get(0)).isEqualTo(userId);
        assertThat(eventCaptor.getValue().getActivityId()).isNotNull();
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(this.userId);

        verify(userSiteService).getAllUserSitesIncludingDeletedOnes(this.userId);

        verify(scrapingDataProviderResponseProcessor).process(any(), eq(Optional.of(userSite)), eq(ProviderServiceResponseStatus.FINISHED), eq(UserSiteActionType.PROVIDER_FLYWHEEL_REFRESH), eq(eventCaptor.getValue().getActivityId()), any(ClientUserToken.class));
    }

    @Test
    void testCallback_AccessMeansExpiredException() throws CallbackIdentifierNotKnownException {
        final String extUserId = "extUserId";
        final String extUserSiteId = "extUserSiteId";
        final UUID internalUserSiteId = randomUUID();
        final UUID activityId = randomUUID();
        UUID siteId = randomUUID();

        PostgresUserSite userSite = new PostgresUserSite(userId, internalUserSiteId, siteId, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, ClientId.random(), "YODLEE", null, null, null, false, null);
        userSite.setExternalId(extUserSiteId);

        ProviderAccountDTO providerAccountDTO = ProviderAccountDTO.builder().accountId("accountid").build();

        UserSiteData userSiteData = new UserSiteData(
                extUserSiteId,
                singletonList(providerAccountDTO),
                emptyList(),
                ProviderServiceResponseStatus.FINISHED);

        UserDataCallbackResponse userDataCallbackResponse = new UserDataCallbackResponse("BUDGET_INSIGHT", extUserId, userSiteData);

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", extUserId))
                .thenReturn(Optional.of(new UserExternalId(userId, "BUDGET_INSIGHT", extUserId)));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(userId)).thenReturn(singletonList(userSite));

        when(userSiteService.checkLock(userSite))
                .thenReturn(Optional.of(new PostgresUserSiteLock(internalUserSiteId, activityId, null)));
        when(providerRequestRepository.find(userId, activityId)).thenReturn(Collections.singletonList(new ProviderRequest(randomUUID(), activityId, userId, internalUserSiteId, UserSiteActionType.CREATE_USER_SITE)));
        providerCallbackService.processCallback(userDataCallbackResponse);
        verify(userSiteService).getAllUserSitesIncludingDeletedOnes(userId);
    }

    @Test
    void testCallback_AccessMeansExpiredExceptionNoAccessMeans() throws CallbackIdentifierNotKnownException {
        final String extUserId = "extUserId";
        final String extUserSiteId = "extUserSiteId";
        final UUID internalUserSiteId = randomUUID();
        final UUID activityId = randomUUID();
        UUID siteId = randomUUID();

        PostgresUserSite userSite = new PostgresUserSite(userId, internalUserSiteId, siteId, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, ClientId.random(), "YODLEE", null, null, null, false, null);
        userSite.setExternalId(extUserSiteId);

        ProviderAccountDTO providerAccountDTO = ProviderAccountDTO.builder().accountId("accountid").build();

        UserSiteData userSiteData = new UserSiteData(
                extUserSiteId,
                singletonList(providerAccountDTO),
                emptyList(),
                ProviderServiceResponseStatus.FINISHED);

        UserDataCallbackResponse userDataCallbackResponse = new UserDataCallbackResponse("BUDGET_INSIGHT", extUserId, userSiteData);

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", extUserId))
                .thenReturn(Optional.of(new UserExternalId(userId, "BUDGET_INSIGHT", extUserId)));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(userId)).thenReturn(singletonList(userSite));

        when(userSiteService.checkLock(userSite))
                .thenReturn(Optional.of(new PostgresUserSiteLock(internalUserSiteId, activityId, null)));
        when(providerRequestRepository.find(userId, activityId)).thenReturn(Collections.singletonList(new ProviderRequest(randomUUID(), activityId, userId, internalUserSiteId, UserSiteActionType.CREATE_USER_SITE)));
        try {
            providerCallbackService.processCallback(userDataCallbackResponse);
        } finally {
            verify(userSiteService).getAllUserSitesIncludingDeletedOnes(userId);
        }
    }

    @Test
    @SneakyThrows
    void testProcessCallbackDataAsync() {
        String budgetInsightCallback = readFile("data/BI_callback_after_post_new_connection.json");
        String saltEdgeCallback = readFile("data/saltedge_success.json");
        UUID budgetInsightUserId = userId;
        UUID saltedgeUserId = randomUUID();

        ClientId someClientId = ClientId.random();
        final UUID someSiteId = randomUUID();
        when(userService.getUser(budgetInsightUserId)).thenReturn(Optional.of(new User(budgetInsightUserId, Instant.now(systemUTC()), someClientId, StatusType.ACTIVE, false)));
        when(userService.getUser(saltedgeUserId)).thenReturn(Optional.of(new User(saltedgeUserId, Instant.now(systemUTC()), someClientId, StatusType.ACTIVE, false)));

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", "59"))
                .thenReturn(Optional.of(new UserExternalId(budgetInsightUserId, "BUDGET_INSIGHT", "59")));
        when(userExternalIdRepository.findByProviderAndExternalUserId("SALTEDGE", "0"))
                .thenReturn(Optional.of(new UserExternalId(saltedgeUserId, "SALTEDGE", "0")));

        PostgresUserSite userSiteFromClientBudgetInsight = new PostgresUserSite();
        userSiteFromClientBudgetInsight.setClientId(someClientId);
        userSiteFromClientBudgetInsight.setProvider("BUDGET_INSIGHT");
        userSiteFromClientBudgetInsight.setSiteId(someSiteId);
        userSiteFromClientBudgetInsight.setUserSiteId(randomUUID());
        PostgresUserSite userSiteFromClientSaltedge = new PostgresUserSite();
        userSiteFromClientSaltedge.setClientId(someClientId);
        userSiteFromClientSaltedge.setProvider("SALTEDGE");
        userSiteFromClientSaltedge.setSiteId(someSiteId);
        userSiteFromClientSaltedge.setUserSiteId(randomUUID());
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(budgetInsightUserId)).thenReturn(List.of(userSiteFromClientBudgetInsight));
        when(userSiteService.getAllUserSitesIncludingDeletedOnes(saltedgeUserId)).thenReturn(List.of(userSiteFromClientSaltedge));
        ClientUserToken budgetInsightClientUserToken = mock(ClientUserToken.class);
        when(callbacksClientTokenService.getClientUserToken(someClientId.unwrap(), budgetInsightUserId)).thenReturn(budgetInsightClientUserToken);
        ClientUserToken saltedgeClientUserToken = mock(ClientUserToken.class);
        when(callbacksClientTokenService.getClientUserToken(someClientId.unwrap(), saltedgeUserId)).thenReturn(saltedgeClientUserToken);

        providerCallbackService.processCallbackDataAsync("BUDGET_INSIGHT", null, budgetInsightCallback);

        verify(formProviderRestClient).processCallback(eq("BUDGET_INSIGHT"), any(), eq(budgetInsightClientUserToken));

        providerCallbackService.processCallbackDataAsync("SALTEDGE", null, saltEdgeCallback);
        verify(formProviderRestClient).processCallback(eq("SALTEDGE"), any(), eq(saltedgeClientUserToken));
    }

    @Test
    void testProcessCallbackDataAsync_UserBlocked() throws IOException, URISyntaxException {
        String budgetInsightCallback = readFile("data/BI_callback_after_post_new_connection.json");
        UUID budgetInsightUserId = randomUUID();
        ClientId someClientId = ClientId.random();
        when(userService.getUser(budgetInsightUserId)).thenReturn(Optional.of(new User(budgetInsightUserId, Instant.now(systemUTC()), someClientId, StatusType.BLOCKED, false)));

        when(userExternalIdRepository.findByProviderAndExternalUserId("BUDGET_INSIGHT", "59"))
                .thenReturn(Optional.of(new UserExternalId(budgetInsightUserId, "BUDGET_INSIGHT", "59")));

        PostgresUserSite userSiteFromClient = new PostgresUserSite();
        userSiteFromClient.setClientId(someClientId);

        providerCallbackService.processCallbackDataAsync("BUDGET_INSIGHT", null, budgetInsightCallback);
        verifyNoInteractions(formProviderRestClient);
    }

    @Test
    void processNoSupportedAccountsFromCallback_noUserSiteLock() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        ClientId clientId = ClientId.random();
        PostgresUserSite userSite = PostgresUserSite.builder()
                .userId(userId)
                .userSiteId(userSiteId)
                .clientId(clientId)
                .build();

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setClaim("user-id", userId.toString());
        when(callbacksClientTokenService.getClientUserToken(clientId.unwrap(), userId)).thenReturn(new ClientUserToken("serializedCT", jwtClaims));

        when(userSiteService.getUserSite(userId, userSiteId)).thenReturn(userSite);
        when(userSiteService.checkLock(userSite)).thenReturn(Optional.empty());

        providerCallbackService.processNoSupportedAccountsFromCallback(userId, userSiteId);

        ArgumentCaptor<RefreshUserSitesFlywheelEvent> refreshUserSitesFlywheelEventArgumentCaptor = ArgumentCaptor.forClass(RefreshUserSitesFlywheelEvent.class);
        verify(activityService).startActivity(any(), refreshUserSitesFlywheelEventArgumentCaptor.capture());
        RefreshUserSitesFlywheelEvent refreshUserSitesFlywheelEvent = refreshUserSitesFlywheelEventArgumentCaptor.getValue();
        assertThat(refreshUserSitesFlywheelEvent.getUserId()).isEqualTo(userId);
        assertThat(refreshUserSitesFlywheelEvent.getUserSiteIds()).containsExactly(userSiteId);

        verify(scrapingDataProviderResponseProcessor).processNoSupportedAccountsMessage(userId, userSiteId, UserSiteActionType.PROVIDER_FLYWHEEL_REFRESH);
    }

    @Test
    void processNoSupportedAccountsFromCallback_noProviderRequests() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();
        PostgresUserSite userSite = PostgresUserSite.builder()
                .userId(userId)
                .userSiteId(userSiteId)
                .clientId(ClientId.random())
                .build();
        PostgresUserSiteLock userSiteLock = mock(PostgresUserSiteLock.class);

        when(userSiteService.getUserSite(userId, userSiteId)).thenReturn(userSite);
        when(userSiteService.checkLock(userSite)).thenReturn(Optional.of(userSiteLock));
        when(userSiteLock.getActivityId()).thenReturn(activityId);
        when(providerRequestRepository.find(userId, activityId)).thenReturn(emptyList());

        providerCallbackService.processNoSupportedAccountsFromCallback(userId, userSiteId);

        verify(scrapingDataProviderResponseProcessor).processNoSupportedAccountsMessage(userId, userSiteId, UserSiteActionType.PROVIDER_CALLBACK);
    }

    @Test
    void processNoSupportedAccountsFromCallback_existedProviderRequests() {
        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID activityId = randomUUID();
        PostgresUserSite userSite = PostgresUserSite.builder()
                .userId(userId)
                .userSiteId(userSiteId)
                .clientId(ClientId.random())
                .build();

        PostgresUserSiteLock userSiteLock = mock(PostgresUserSiteLock.class);
        ProviderRequest providerRequest = mock(ProviderRequest.class);

        when(userSiteService.getUserSite(userId, userSiteId)).thenReturn(userSite);
        when(userSiteService.checkLock(userSite)).thenReturn(Optional.of(userSiteLock));
        when(userSiteLock.getActivityId()).thenReturn(activityId);
        when(providerRequestRepository.find(userId, activityId)).thenReturn(singletonList(providerRequest));
        when(providerRequest.getUserSiteActionType()).thenReturn(UserSiteActionType.USER_REFRESH);

        providerCallbackService.processNoSupportedAccountsFromCallback(userId, userSiteId);

        verify(scrapingDataProviderResponseProcessor).processNoSupportedAccountsMessage(userId, userSiteId, UserSiteActionType.USER_REFRESH);
    }

    private String readFile(String filename) throws IOException, URISyntaxException {
        return String.join("\n", Files.readAllLines(new File(getClass().getClassLoader().getResource(filename).toURI()).toPath(), StandardCharsets.UTF_8));
    }
}
