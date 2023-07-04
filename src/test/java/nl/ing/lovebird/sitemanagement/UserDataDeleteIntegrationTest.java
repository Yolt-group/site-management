package nl.ing.lovebird.sitemanagement;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeans;
import nl.ing.lovebird.sitemanagement.accessmeans.UserSiteAccessMeans;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsent;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import nl.ing.lovebird.sitemanagement.providerclient.ApiNotifyUserSiteDeleteDTO;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.consentsession.GeneratedSessionState;
import nl.ing.lovebird.testsupport.cassandra.DeleteUserDataExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.accessmeans.AesEncryptionUtil.encrypt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestContext
class UserDataDeleteIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID URL_USER_SITE_ID = UUID.randomUUID();
    private static final UUID FORM_USER_SITE_ID = UUID.randomUUID();
    private static final UUID DEFAULT_ID = UUID.randomUUID();
    private static final String FORM_PROVIDER = "YODLEE";
    private static final String URL_PROVIDER = "MONZO";

    private static final String[] EXCLUDED_TABLES = {
            // don't have user_id as first partition key
            "client_redirect_url",
            "client_site",
            "modelmutation",
            "orphan_user",
            "orphan_user_batch",
            "orphan_user_external_id",
            "popular_country_site_ranking",
            "provider_maintenance",
            "site_consent_template",
            "site_login_form",
            "site_v2",

            // should never be removed as it is used for audit and tracking purposes
            "user_external_id",
            "user_site_delete",
            "site_consent",
            "account_migration",
            "account",

            // hard to properly delete at the moment. Does not contain sensitive information and has a TTL, so will be removed 'eventually'
            "user_site_session",
            "user_site_lock",
            "provider_request_v3",

            // users send delete user event via kafka, check UserUpdateService#consume(UserUpdateDTO, Optional<UserContext>)
            "user",

            // no longer filled
            "user_site_action"
    };

    @Autowired
    private Clock clock;

    @Autowired
    private Session session;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderRestClient providerRestClientMock;

    @Autowired
    private ClientTokenRequesterService clientTokenRequesterService;

    @Autowired
    private MaintenanceClient maintenanceClientMock;

    @Autowired
    private PostgresUserSiteRepository userSiteRepository;

    @Autowired
    private VaultKeys vaultKeys;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private TestClientTokens testClientTokens;

    private ClientUserToken clientUserToken;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), DEFAULT_ID, USER_ID);
        when(clientTokenRequesterService.getClientUserToken(DEFAULT_ID, USER_ID)).thenReturn(clientUserToken);
        doNothing().when(providerRestClientMock).notifyUserSiteDelete(any(String.class), any(ApiNotifyUserSiteDeleteDTO.class),  eq(clientUserToken), eq(false));
    }

    @AfterEach
    void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    private void prepareData() {
        MappingManager mappingManager = new MappingManager(session);

        // Adding provider sites
        FauxProvidersService.setupProviderSitesStub(wireMockServer,
                new ProvidersSites(
                        List.of(TestProviderSites.MONZO_SITE, TestProviderSites.BARCLAYS_SCRAPING),
                        Collections.emptyList()));
        sitesProvider.update();

        // Adding user-sites

        PostgresUserSite urlUserSite = new PostgresUserSite(USER_ID, URL_USER_SITE_ID, TestProviderSites.MONZO_SITE.getId(), null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, new ClientId(DEFAULT_ID), URL_PROVIDER, null, null, null, false, null);
        urlUserSite.setRedirectUrlId(UUID.randomUUID());
        userSiteRepository.save(urlUserSite);

        PostgresUserSite formUserSite = new PostgresUserSite(USER_ID, FORM_USER_SITE_ID, TestProviderSites.BARCLAYS_SCRAPING.getId(), null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, new ClientId(DEFAULT_ID), FORM_PROVIDER, null, null, null, false, null);
        userSiteRepository.save(formUserSite);

        // Adding access means

        final Date hourIntoTheFuture = Date.from(Instant.now(clock).plus(1, ChronoUnit.HOURS));
        AccessMeans accessMeans = new AccessMeans(USER_ID, FORM_PROVIDER, encrypt("accessMeans", vaultKeys.getSymmetricKey("encryption-key")), Date.from(ZonedDateTime.now(clock).toInstant()), hourIntoTheFuture);
        mappingManager.mapper(AccessMeans.class).save(accessMeans);

        UserSiteAccessMeans userSiteAccessMeans = new UserSiteAccessMeans(USER_ID, URL_USER_SITE_ID, URL_PROVIDER,
                encrypt("userSiteAccessMeans", vaultKeys.getSymmetricKey("encryption-key")), Date.from(ZonedDateTime.now(clock).toInstant()), Date.from(ZonedDateTime.now(clock).toInstant()), Instant.EPOCH);
        mappingManager.mapper(UserSiteAccessMeans.class).save(userSiteAccessMeans);

        // Adding external consents

        ExternalConsent externalConsent1 = new ExternalConsent(USER_ID, TestProviderSites.MONZO_SITE.getId(), URL_USER_SITE_ID, "expiryWeek1", Instant.now(clock),
                Instant.now(clock), "externalConsentId1");
        mappingManager.mapper(ExternalConsent.class).save(externalConsent1);

        // Adding generated state
        mappingManager.mapper(GeneratedSessionState.class).save(new GeneratedSessionState(USER_ID, UUID.randomUUID().toString(), Date.from(ZonedDateTime.now(clock).toInstant()), true, UUID.randomUUID()));
    }

    @Test
    @SneakyThrows
    void shouldDeleteAllUserData() {
        prepareData();

        // XXX This looks like a junit5 extension but it's not how you're supposed to use it judging by the code in lbc, kinda weird ....
        DeleteUserDataExtension deleteUserDataExtension = new DeleteUserDataExtension();
        deleteUserDataExtension.setUserId(USER_ID);
        deleteUserDataExtension.setSession(session);
        deleteUserDataExtension.setExcludedTables(EXCLUDED_TABLES);

        deleteUserDataExtension.beforeTestExecution(null);

        mockMvc.perform(delete("/delete-user/" + USER_ID)
                        .header("client-token", clientUserToken.getSerialized()))
                .andExpect(status().isNoContent());
        verify(maintenanceClientMock).scheduleUserSiteDelete(USER_ID, URL_USER_SITE_ID);
        verify(maintenanceClientMock).scheduleUserSiteDelete(USER_ID, FORM_USER_SITE_ID);

        mockMvc.perform(delete("/user-sites/" + USER_ID + "/" + URL_USER_SITE_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/user-sites/" + USER_ID + "/" + FORM_USER_SITE_ID))
                .andExpect(status().isOk());

        deleteUserDataExtension.afterTestExecution(null);
    }

}
