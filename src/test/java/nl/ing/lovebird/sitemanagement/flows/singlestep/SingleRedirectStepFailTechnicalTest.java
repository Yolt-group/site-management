package nl.ing.lovebird.sitemanagement.flows.singlestep;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.Value;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSitesProvider;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.*;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.LoginResponseDTO;
import nl.ing.lovebird.sitemanagement.usersite.LoginStepV1DTO;
import nl.ing.lovebird.sitemanagement.usersite.UrlLoginDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.DISCONNECTED;
import static nl.ing.lovebird.sitemanagement.usersite.FailureReason.AUTHENTICATION_FAILED;
import static nl.ing.lovebird.sitemanagement.usersite.FailureReason.TECHNICAL_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.post;

/**
 * This test covers all places where site-management can encounter a technical error [1] when communicating with other
 * yolt services.  We make some assumptions:
 * - if we get a 200 message from providers when we ask to fetch data, we assume that a kafka message will be sent by providers at some point
 * - if providers can fetch data and sends us an OK over kafka, we assume that A&T will also deliver its IngestionFinished event without fail
 * <p>
 * There are several possible places where a **technical** error can occur:
 * - when the client retrieves the url to send the user to with {@link #postConnect}:
 * 1a. providers fails to make a login url
 * - when the client posts back the consent url
 * 2a. providers fails to create accessMeans
 * 2b. providers fails to acknowledge the request for fetching data
 * <p>
 * [1] a technical error is anything non-functional, think: 500 status codes / timeouts / connection failures
 * <p>
 * For an overview of the available statuscode and failure reason combinations refer to: https://developer.yolt.com/docs/scenarios
 */
@IntegrationTestContext
public class SingleRedirectStepFailTechnicalTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private ClientSitesProvider clientSitesProvider;

    @Autowired
    private ClientRedirectUrlService clientRedirectUrlService;
    @Autowired
    private TestClientTokens testClientTokens;
    OnboardedSiteContext flowContext = new OnboardedSiteContext();

    /**
     * 1a. providers fails to make a login url
     */
    @Test
    public void scenario_1a() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        final String provider = flowContext.clientSite().getSite().getProvider();

        // site-management will make a call to providers to retrieve the login information for the specific bank (providers will construct an URL)
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/login-info\\?(.*)"))
                .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

        ResponseEntity<Map<String, String>> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s",
                        flowContext.user().getId(),
                        flowContext.clientSite().getSiteId(),
                        flowContext.clientSite().getAISRedirectUrlId()
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), new ParameterizedTypeReference<>() {
                });

        assertThat(resp.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().get("code")).isEqualTo("SM034");
        assertThat(resp.getBody().get("message")).isEqualTo("Got an error from providers.");
    }

    /**
     * 2a. providers fails to create accessMeans
     */
    @Test
    public void scenario_2a() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);
        final String provider = flowContext.clientSite().getSite().getProvider();

        var postConnectResult = postConnect(provider);
        String urlPostedBackToUsByClientAfterUserGaveConsent = flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(postConnectResult.redirectUrlFromProviders).getState();

        // site-management will make a call to providers for the token exchange, we make providers fail
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/access-means/create\\?(.*)"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        UrlLoginDTO body = new UrlLoginDTO();
        body.setRedirectUrl(urlPostedBackToUsByClientAfterUserGaveConsent);
        ResponseEntity<LoginResponseDTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginResponseDTOAssertions.assertInvariants(resp.getBody());
        assertThat(resp.getBody().getActivityId())
                .withFailMessage("An activity should not have been started.")
                .isNull();
        assertThat(resp.getBody().getStep())
                .withFailMessage("Step should not be present.")
                .isNull();

        UserSiteDTOAssertions.assertStatusAndReason(resp.getBody().getUserSite(), DISCONNECTED, AUTHENTICATION_FAILED);
    }

    /**
     * 2b. providers fails to acknowledge the request for fetching data
     */
    @Test
    public void scenario_2b() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);
        final String provider = flowContext.clientSite().getSite().getProvider();

        var postConnectResult = postConnect(provider);
        String urlPostedBackToUsByClientAfterUserGaveConsent = flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(postConnectResult.redirectUrlFromProviders).getState();

        // site-management will make a call to providers for the token exchange, we make providers respond with valid AccessMeans
        FauxProvidersService.setupPostAccessMeansCreateStubForAccessMeans(wireMockServer, provider,
                apiCreateAccessMeansDTO -> true,
                () -> Instant.now(systemUTC()).plus(90, DAYS)
        );
        // site-management will ask accounts-and-transactions for the user site transaction summary.
        FauxAccountsAndTransactionsService.setupUserSiteTransactionStatusSummary(wireMockServer);

        // site-management will make a call to providers to trigger a data fetch, make it fail
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/" + provider + "/fetch-data\\?(.*)"))
                .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

        UrlLoginDTO body = new UrlLoginDTO();
        body.setRedirectUrl(urlPostedBackToUsByClientAfterUserGaveConsent);
        ResponseEntity<LoginResponseDTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginResponseDTOAssertions.assertInvariants(resp.getBody());
        assertThat(resp.getBody().getActivityId())
                // This looks strange but is correct, if we have >= 2 user-sites and 1 call to providers succeeds, and the other fails: there is an activity in progress.
                .withFailMessage("An activity should have been started.")
                .isNotNull();
        assertThat(resp.getBody().getStep())
                .withFailMessage("Step should not be present.")
                .isNull();

        UserSiteDTOAssertions.assertDataFetchNotCompletedWithReason(resp.getBody().getUserSite(), TECHNICAL_ERROR);
    }

    private PostConnectResult postConnect(String provider) {
        // site-management will make a call to providers to retrieve the login information for the specific bank (providers will construct an URL)
        FauxProvidersService.setupPostLoginInfoStubForRedirectStep(wireMockServer, provider, (baseClientRedirectUrl, state) -> "https://abnamro.example.com?redirect_uri=" + baseClientRedirectUrl + "&state=" + state);

        ResponseEntity<LoginStepV1DTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s",
                        flowContext.user().getId(),
                        flowContext.clientSite().getSiteId(),
                        flowContext.clientSite().getAISRedirectUrlId()
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), LoginStepV1DTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginStepV1DTO dto = resp.getBody();
        LoginStepV1DTOAssertions.assertIsRedirectStep(dto);

        return new PostConnectResult(dto.getUserSiteId(), dto.getRedirect().getUrl());
    }

    @Value
    private static class PostConnectResult {

        UUID userSiteId;
        String redirectUrlFromProviders;
    }

    @AfterEach
    public void tearDown() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }
}
