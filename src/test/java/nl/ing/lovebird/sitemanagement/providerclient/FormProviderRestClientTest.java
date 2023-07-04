package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import nl.ing.lovebird.logging.MDCContextCreator;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class FormProviderRestClientTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final UUID USER_ID = UUID.randomUUID();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .setDateFormat(new StdDateFormat().withColonInTimeZone(false))
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule())
            .registerModule(new JsonComponentModule());

    static WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    private FormProviderRestClient restClient;

    private ClientToken clientToken;

    @BeforeEach
    void setUp() {
        restClient = new FormProviderRestClient(new RestTemplateBuilder(), "http://localhost:" + wireMockServer.port() + "/site-management", 122, new ErrorCodeExtractor(objectMapper));
        MDC.put(MDCContextCreator.USER_ID_MDC_KEY, USER_ID.toString());
        MDC.put(MDCContextCreator.CLIENT_ID_MDC_KEY, CLIENT_ID.toString());

        JwtClaims clientClaims = TestJwtClaims.createClientClaims("junit", UUID.randomUUID(), CLIENT_ID.unwrap());
        clientToken = new ClientToken("mocked-client-token-" + CLIENT_ID, clientClaims);
    }

    @Test
    @SneakyThrows
    public void testFetchProviderExternalUserIds() {
        UUID expectedResult = UUID.randomUUID();
        wireMockServer.stubFor(any(urlMatching("/site-management/form/[^/]+/external-user-ids"))
                .withHeader("client-token", equalTo(clientToken.getSerialized()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody(expectedResult.toString())));

        UUID actualResult = restClient.fetchProviderExternalUserIds("BUDGET_INSIGHT", clientToken);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    @SneakyThrows
    public void testDeleteOrphanUserAtProvider() {
        wireMockServer.stubFor(any(urlMatching("/site-management/form/[^/]+/external-user-ids/[^/]+"))
                .withHeader("client-token", equalTo(clientToken.getSerialized()))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(HttpStatus.ACCEPTED.value())));

        restClient.deleteOrphanUserAtProvider("BUDGET_INSIGHT", "ext2", clientToken);

        // should not throw exceptions
    }

}
