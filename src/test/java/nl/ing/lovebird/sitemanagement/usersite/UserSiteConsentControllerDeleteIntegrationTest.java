package nl.ing.lovebird.sitemanagement.usersite;

import com.datastax.driver.core.Session;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.NonNull;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeans;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxAccountsAndTransactionsService;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.site.SiteDTO;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites.ABN_AMRO;
import static nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites.AIB;
import static nl.ing.lovebird.testsupport.cassandra.CassandraHelper.truncate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@IntegrationTestContext
class UserSiteConsentControllerDeleteIntegrationTest {

    private final UUID clientGroupId = UUID.randomUUID();
    private static final ClientId CLIENT_ID = TestUtil.YOLT_APP_CLIENT_ID;
    private static final String EXTERNAL_ID_1 = "external-id1";
    private static final String EXTERNAL_ID_2 = "external-id2";
    private final UUID userId = UUID.randomUUID();

    private CassandraHelper.OpenRepository<AccessMeans> accessMeansCassandra;

    @Autowired
    private Clock clock;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserSiteService userSiteService;
    @Autowired
    private PostgresUserSiteRepository userSiteRepository;
    @Autowired
    private Session session;
    @Autowired
    private MaintenanceClient maintenanceClient;
    @Autowired
    private FormProviderRestClient formProviderRestClient;
    @Autowired
    private ClientTokenRequesterService clientTokenRequesterService;
    @Autowired
    private SitesProvider sitesProvider;
    @Autowired
    private WireMockServer wireMockServer;
    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    void setUp() {
        truncate(session, AccessMeans.class);
        accessMeansCassandra = CassandraHelper.openRepository(session, AccessMeans.class);

        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), CLIENT_ID.unwrap(), userId);

        when(clientTokenRequesterService.getClientUserToken(eq(CLIENT_ID.unwrap()), any())).thenReturn(clientUserToken);

        FauxProvidersService.setupProviderSitesStub(wireMockServer, createProviderSites());
        sitesProvider.update();
    }

    /**
     * TODO C4PO-7504 should be changed after popular conuntries are moved to providers
     * ids of 2 popular countries were chosen to satisfy tests
     */
    private ProvidersSites createProviderSites() {
        return new ProvidersSites(List.of(
                ABN_AMRO,
                AIB,
                TestProviderSites.LLOYDS,
                TestProviderSites.BARCLAYS_SCRAPING),
                Collections.emptyList()
        );
    }

    @AfterEach
    public void cleanup() {
        truncate(session, AccessMeans.class);

        verifyNoMoreInteractions(maintenanceClient);

        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    void markUserSiteForDeletion_urlProvider() throws Exception {
        final UUID userSiteId1 = UUID.randomUUID();
        @NonNull @NotNull Date created1 = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId1, ABN_AMRO.getId(), EXTERNAL_ID_1, ConnectionStatus.CONNECTED, null, null, created1.toInstant(), null, null, CLIENT_ID, "STARLINGBANK", null, UUID.randomUUID(), null, false, null));

        final UUID userSiteId2 = UUID.randomUUID();
        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId2, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        FauxAccountsAndTransactionsService.setupUserAccounts(wireMockServer, userId);

        assertUserSiteIsPresent(userId, userSiteId1);

        markUserSiteForDeletionIsOk(userId, userSiteId1);

        assertUserSiteIsMarked(userId, userSiteId1);
        assertNoAccessMeans();
        assertUserSiteIsDeleted(userId, userSiteId1);
        verifyClientsCalled(userId, userSiteId1);
    }

    @Test
    void markUserSiteForDeletion_formProvider() throws Exception {
        final UUID userSiteId1 = UUID.randomUUID();

        @NonNull @NotNull Date created1 = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId1, ABN_AMRO.getId(), EXTERNAL_ID_1, ConnectionStatus.CONNECTED, null, null, created1.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        final UUID userSiteId2 = UUID.randomUUID();
        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId2, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        FauxAccountsAndTransactionsService.setupUserAccounts(wireMockServer, userId);

        createYodleeAccessMeans(userId);

        assertUserSiteIsPresent(userId, userSiteId2);

        markUserSiteForDeletionIsOk(userId, userSiteId2);

        assertUserSiteIsMarked(userId, userSiteId2);

        assertUserSiteIsDeleted(userId, userSiteId2);

        verifyClientsCalled(userId, userSiteId2);
    }

    @Test
    void markAndDeleteUserSite_formProvider() throws Exception {
        final UUID userSiteId1 = UUID.randomUUID();

        @NonNull @NotNull Date created1 = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId1, ABN_AMRO.getId(), EXTERNAL_ID_1, ConnectionStatus.CONNECTED, null, null, created1.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        final UUID userSiteId2 = UUID.randomUUID();
        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId2, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        FauxAccountsAndTransactionsService.setupUserAccounts(wireMockServer, userId);

        createYodleeAccessMeans(userId);

        assertUserSiteIsPresent(userId, userSiteId2);

        markUserSiteForDeletionIsOk(userId, userSiteId2);

        deleteUserSiteIsOk(userId, userSiteId2);

        verifyClientsCalled(userId, userSiteId2);

        // first time it gets called asynchronously during markToDelete, second time it's called it's directly
        verify(formProviderRestClient, times(2)).deleteUserSite(eq("YODLEE"), any(), any(), any());
    }

    @Test
    void deleteUserSite_formProvider() throws Exception {
        final UUID userSiteId1 = UUID.randomUUID();

        @NonNull @NotNull Date created1 = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId1, ABN_AMRO.getId(), EXTERNAL_ID_1, ConnectionStatus.CONNECTED, null, null, created1.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        final UUID userSiteId2 = UUID.randomUUID();
        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId2, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, true, null));

//        userSiteDeleteCassandra.save(new UserSiteDelete(userId, userSiteId2, EXTERNAL_ID_2, new Date()));

        createYodleeAccessMeans(userId);

        assertUserSiteIsDeleted(userId, userSiteId2);

        deleteUserSiteIsOk(userId, userSiteId2);

        verify(formProviderRestClient).deleteUserSite(eq("YODLEE"), any(), any(), any());
    }

    @Test
    void deleteUserSite_formProvider_lastUserSiteForProvider() throws Exception {
        final UUID userSiteId = UUID.randomUUID();

        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, true, null));

        createYodleeAccessMeans(userId);

        assertUserSiteIsDeleted(userId, userSiteId);

        deleteUserSiteIsOk(userId, userSiteId);

        verify(formProviderRestClient).deleteUserSite(eq("YODLEE"), any(), any(), any());
        verify(formProviderRestClient).deleteUser(eq("YODLEE"), any(), any());
    }

    private void createYodleeAccessMeans(UUID userId) {
        // input:"access-means", secretKey:"4060b34b5c66ea2de14c3cfd03c12ffce35697bc8fd1ac863bd7e27b3deb78ee" (AesEncryptionUtilTest.secretKey)
        String accessMeansEncrypted =
                "a9521e07f3f39b71b7b4ad8402a1888ae93ef580f92e385ed64977648b4b368b8915a9ec305e4484a8cc7a9fa7252ad9f88ed4c376ddd4f5a6558d45";
        AccessMeans accessMeans = new AccessMeans(userId, "YODLEE", accessMeansEncrypted, new Date(), Date.from(ZonedDateTime.now(clock).plusYears(1).toInstant()));
        accessMeansCassandra.save(accessMeans);
    }

    @Test
    void deleteUser() throws Exception {
        final UUID userSiteId1 = UUID.randomUUID();
        @NonNull @NotNull Date created1 = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId1, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created1.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        final UUID userSiteId2 = UUID.randomUUID();
        @NonNull @NotNull Date created = Date.from(Instant.now(clock));
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId2, AIB.getId(), EXTERNAL_ID_2, ConnectionStatus.CONNECTED, null, null, created.toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null));

        accessMeansCassandra.save(new AccessMeans(userId, "YODLEE", "access-means", new Date(), Date.from(ZonedDateTime.now(clock).plusYears(1).toInstant())));

        assertUserSiteIsPresent(userId, userSiteId1);
        assertUserSiteIsPresent(userId, userSiteId2);

        deleteUserIsNoContent(userId);
        assertUserSiteIsDeleted(userId, userSiteId1);
        assertUserSiteIsDeleted(userId, userSiteId2);
        verify(maintenanceClient).scheduleUserSiteDelete(userId, userSiteId1);
        verify(maintenanceClient).scheduleUserSiteDelete(userId, userSiteId2);

    }

    private void deleteUserIsNoContent(UUID userId) throws Exception {
        final String url = "/delete-user/" + userId.toString();
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("client-token", clientUserToken.getSerialized());
        HttpEntity<?> httpEntity = createHttpEntity(userId, headers, null);
        ResponseEntity<Void> response = restTemplate.exchange(new URI(url), HttpMethod.DELETE, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    }

    private void deleteUserSiteIsOk(UUID userId, final UUID userSiteId2) throws Exception {
        final String url = "/user-sites/" + userId.toString() + "/" + userSiteId2;

        HttpEntity<?> httpEntity = createEmptyHttpEntity(userId);
        ResponseEntity<Void> response = restTemplate.exchange(new URI(url), HttpMethod.DELETE, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    private void markUserSiteForDeletionIsOk(final UUID userId, final UUID userSiteId2) throws Exception {
        final String url = "/user-sites/" + userSiteId2;

        HttpEntity<?> httpEntity = createEmptyHttpEntity(userId);
        ResponseEntity<Void> response = restTemplate.exchange(new URI(url), HttpMethod.DELETE, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }


    private void assertNoAccessMeans() {
        assertThat(accessMeansCassandra.selectAll()).isEmpty();
    }

    private void assertUserSiteIsDeleted(final UUID userId, final UUID userSiteId2) throws Exception {
        final String url = "/user-sites/" + userSiteId2;

        HttpEntity<?> httpEntity = createEmptyHttpEntity(userId);
        ResponseEntity<SiteDTO> response = restTemplate.exchange(new URI(url), HttpMethod.GET, httpEntity, SiteDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    }

    private void assertUserSiteIsPresent(final UUID userId, final UUID userSiteId) throws Exception {
        final String url = "/user-sites/" + userSiteId;
        HttpEntity<?> httpEntity = createEmptyHttpEntity(userId);
        ResponseEntity<SiteDTO> response = restTemplate.exchange(new URI(url), HttpMethod.GET, httpEntity, SiteDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(userSiteId);
    }

    private void verifyClientsCalled(UUID userId, final UUID userSiteId) {
        verify(maintenanceClient).scheduleUserSiteDelete(userId, userSiteId);
    }

    private void assertUserSiteIsMarked(UUID userId, final UUID userSiteId) {
        assertThatThrownBy(() -> userSiteService.getUserSite(userId, userSiteId))
                .isInstanceOf(UserSiteNotFoundException.class)
                .hasMessage("User site " + userSiteId.toString() + " is marked for deletion.");
    }

    private HttpEntity<?> createEmptyHttpEntity(UUID userId) {
        HttpHeaders requestHeaders = new HttpHeaders();
        return createHttpEntity(userId, requestHeaders, null);
    }

    private HttpEntity<?> createHttpEntity(UUID userId, HttpHeaders requestHeaders, Object body) {
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(clientGroupId, CLIENT_ID.unwrap(), userId);
        requestHeaders.add("client-token", clientUserToken.getSerialized());
        requestHeaders.add("user-id", userId.toString());
        return new HttpEntity<>(body, requestHeaders);
    }
}
