package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.logging.MDCContextCreator;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.RedirectStep;
import nl.ing.lovebird.sitemanagement.usersite.Step;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderRestClientTest {

    private static final Long HOUR_IN_MILLISECS = 1000L * 60L * 60L;
    private static final ClientId CLIENT_ID = ClientId.random();
    private static final ClientUserToken CLIENT_TOKEN = mock(ClientUserToken.class);
    private static final UUID CLIENT_REDIRECT_URL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final UUID SITE_ID = UUID.randomUUID();
    private static final String STALE_ACCESS_MEANS = UUID.randomUUID().toString();
    private static final String ACCESS_MEANS = UUID.randomUUID().toString();

    private final ObjectMapper objectMapper = new ObjectMapper();

    static WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @BeforeAll
    public static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    public static void stopWireMock() {
        wireMockServer.stop();
    }

    private ProviderRestClient providerRestClient;

    @BeforeEach
    public void setup() throws IOException {
        objectMapper.registerModule(new JavaTimeModule());
        providerRestClient =
                new ProviderRestClient(new RestTemplateBuilder(), "http://localhost:" + wireMockServer.port() + "/provider", objectMapper, new ErrorCodeExtractor(objectMapper));
        MDC.put(MDCContextCreator.USER_ID_MDC_KEY, USER_ID.toString());
        MDC.put(MDCContextCreator.CLIENT_ID_MDC_KEY, CLIENT_ID.toString());
        when(CLIENT_TOKEN.getSerialized()).thenReturn("serialized-client-token");
        when(CLIENT_TOKEN.getUserIdClaim()).thenReturn(USER_ID);
        when(CLIENT_TOKEN.getClientIdClaim()).thenReturn(CLIENT_ID.unwrap());
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.resetAll();
    }

    @Test
    void testRefreshAccessMeansOk() throws Exception {

        // Mock it
        AccessMeansDTO expectedResult = makeAccessMeansDTO();
        wireMockServer.stubFor(any(urlMatching("/provider/[^/]+/access-means/refresh\\?(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(writeJson(expectedResult))));

        // Prep it
        Date staleUpdate = new Date(System.currentTimeMillis() - (HOUR_IN_MILLISECS * 24L));
        Date staleExpires = new Date(System.currentTimeMillis() - (HOUR_IN_MILLISECS));
        AccessMeansDTO staleAccessMeansDTO = new AccessMeansDTO(USER_ID, STALE_ACCESS_MEANS, staleUpdate, staleExpires);
        AuthenticationMeansReference authenticationMeanReference = new AuthenticationMeansReference(CLIENT_ID.unwrap(), CLIENT_REDIRECT_URL_ID);
        RefreshAccessMeansDTO refreshAccessMeansDTO = new RefreshAccessMeansDTO(staleAccessMeansDTO, authenticationMeanReference, null, Instant.now().minus(30,ChronoUnit.DAYS));

        // Do it
        AccessMeansDTO accessMeansDTO = providerRestClient.refreshAccessMeans(
                "YOLT_PROVIDER", SITE_ID, refreshAccessMeansDTO, null, false
        );

        // Check it
        assertThat(accessMeansDTO).isNotNull();
        assertThat(accessMeansDTO).isEqualTo(expectedResult);

        wireMockServer.verify(postRequestedFor(urlMatching("/provider/YOLT_PROVIDER/access-means/refresh\\?(.*)"))
                .withRequestBody(equalToJson(writeJson(refreshAccessMeansDTO))));
    }

    @Test
    @SneakyThrows
    public void testCreateAccessMeansOk() {

        // Mock it
        AccessMeansDTO expectedAccessMeansDTO = makeAccessMeansDTO();
        AccessMeansOrStepDTO expectedResult = new AccessMeansOrStepDTO(expectedAccessMeansDTO);
        wireMockServer.stubFor(any(urlMatching("/provider/v2/[^/]+/access-means/create\\?(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(writeJson(expectedResult))));

        // Prep it
        AuthenticationMeansReference authenticationMeanReference = new AuthenticationMeansReference(CLIENT_ID.unwrap(), CLIENT_REDIRECT_URL_ID);
        ApiCreateAccessMeansDTO requestDTO = new ApiCreateAccessMeansDTO(USER_ID, authenticationMeanReference, "", null, "", UUID.randomUUID(), "", "baseRedirect.com");

        // Do it
        AccessMeansDTO accessMeansDTO = providerRestClient.createNewAccessMeans(
                "YOLT_PROVIDER", SITE_ID, requestDTO, null, false
        ).getAccessMeans();

        // Check it
        assertThat(accessMeansDTO).isNotNull();
        assertThat(accessMeansDTO).isEqualTo(expectedAccessMeansDTO);

        wireMockServer.verify(postRequestedFor(urlMatching("/provider/v2/YOLT_PROVIDER/access-means/create\\?(.*)"))
                .withRequestBody(equalToJson(writeJson(requestDTO))));
    }

    @Test
    @SneakyThrows
    public void testGetLoginInfoOk() {

        // Mock it
        wireMockServer.stubFor(any(urlMatching("/provider/v2/[^/]+/login-info\\?(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"redirectUrl\":\"http://my-test.com/login\", \"externalConsentId\":\"external-consent-id\", \"type\":\"REDIRECT_URL\"}")));

        // Prep it
        AuthenticationMeansReference authenticationMeanReference = new AuthenticationMeansReference(CLIENT_ID.unwrap(), CLIENT_REDIRECT_URL_ID);
        ApiGetLoginDTO requestDTO = new ApiGetLoginDTO("", "", authenticationMeanReference, "externalConsentId", null);

        // Do it
        Step step = providerRestClient.getLoginInfo("YOLT_PROVIDER", SITE_ID, requestDTO,  null, false);

        // Check it
        assertThat(step).isNotNull();
        assertThat(((RedirectStep) step).getRedirectUrl()).isEqualTo("http://my-test.com/login");
        assertThat(((RedirectStep) step).getExternalConsentId()).isEqualTo("external-consent-id");

        wireMockServer.verify(postRequestedFor(urlMatching("/provider/v2/YOLT_PROVIDER/login-info\\?(.*)"))
                .withRequestBody(equalToJson(writeJson(requestDTO))));
    }

    @Test
    void testFetchDataOk() throws Exception {

        // Mock it
        wireMockServer.stubFor(any(urlMatching("/provider/[^/]+/fetch-data\\?(.*)"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(201)));

        // Prep it
        AccessMeansDTO accessMeansDTO = makeAccessMeansDTO();
        AuthenticationMeansReference authenticationMeanReference = new AuthenticationMeansReference(CLIENT_ID.unwrap(), CLIENT_REDIRECT_URL_ID);
        ApiFetchDataDTO requestDTO = new ApiFetchDataDTO(USER_ID, USER_SITE_ID, Instant.now(systemUTC()), accessMeansDTO, authenticationMeanReference, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, new UserSiteDataFetchInformation(null, null, null, Collections.emptyList(), Collections.emptyList()));

        // Do it
        providerRestClient.fetchData("YOLT_PROVIDER", SITE_ID, requestDTO,  null, false);

        // Check it
        wireMockServer.verify(1, postRequestedFor(urlMatching("/provider/YOLT_PROVIDER/fetch-data\\?(.*)"))
                .withRequestBody(equalToJson(writeJson(requestDTO))));
    }

    @Test
    @SneakyThrows
    public void testNotifyUserSiteDeleteOk() {

        // Mock it
        wireMockServer.stubFor(any(urlMatching("/provider/YOLT_PROVIDER/notify-user-site-delete\\?(.*)"))
                .willReturn(noContent()
                        .withBody("")));

        // Prep it
        ApiNotifyUserSiteDeleteDTO requestDTO = new ApiNotifyUserSiteDeleteDTO("external-site-id",
                new AuthenticationMeansReference(CLIENT_ID.unwrap(), CLIENT_REDIRECT_URL_ID), null, null);

        // Do it
        providerRestClient.notifyUserSiteDelete("YOLT_PROVIDER", requestDTO, CLIENT_TOKEN, false);

        // Check it
        wireMockServer.verify(1, postRequestedFor(urlMatching("/provider/YOLT_PROVIDER/notify-user-site-delete\\?(.*)"))
                .withRequestBody(equalToJson(writeJson(requestDTO))));
    }

    private AccessMeansDTO makeAccessMeansDTO() {
        Date refreshedUpdate = new Date();
        Date refreshedExpires = new Date(System.currentTimeMillis() + (HOUR_IN_MILLISECS * 24L));
        return new AccessMeansDTO(USER_ID, ACCESS_MEANS, refreshedUpdate, refreshedExpires);
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
