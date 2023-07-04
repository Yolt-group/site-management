package nl.ing.lovebird.sitemanagement.flows.singlestep;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.Value;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
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
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.post;

/**
 * Connect a user-site for which the bank gives us a token that is only valid for 1 second during
 * {@link #postConnect} to "fake" the situation that a token is close to expiry when we are about
 * to fetch data with {@link #postUserSite}.  This tests the {@link AccessMeansManager} behaviour
 * of renewing the AccessMeans at the provider when it's close to expiry.
 */
@IntegrationTestContext
public class SingleRedirectStepSuccessWithAccessMeansRenewalTest {

    @Autowired
    private Clock clock;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;
    @Autowired
    private SitesProvider sitesProvider;
    @Autowired
    private ClientSitesProvider clientSitesProvider;
    @Autowired
    WireMockServer wireMockServer;
    @Autowired
    private ClientRedirectUrlService clientRedirectUrlService;
    @Autowired
    private TestClientTokens testClientTokens;
    OnboardedSiteContext flowContext = new OnboardedSiteContext();

    @Test
    public void given_consentAccessTokenExpiresInNearFuture_when_triggeringDataFetch_then_manageAccessMeansRefreshesToken() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        // Convenience variable: the provider is used in various places.
        var provider = flowContext.clientSite().getSite().getProvider();

        // Step 1
        var postConnectResult = postConnect(provider);

        // Step 2
        // At this point the client will redirect the user to the url that we provided the client [1].  That url contains
        // a query parameter "state".  The client will give their consent at the bank, the bank will subsequently redirect
        // the user to a webpage owned by the client.  The client will then post the full url [2] to which the user was
        // redirected back to us, this url will contain the same state parameter.  We construct this url here.
        // [1] postConnectResult.redirectUrlFromProviders
        // [2] for the Yolt App such an url might look like "https://www.yolt.com/callback?state=92484886-A556-4A87-978A-AE02E453085B"
        var urlPostedBackToUsByClientAfterUserGaveConsent = flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(postConnectResult.redirectUrlFromProviders).getState();

        // Step 3
        var loginResponseDTO = postUserSite(provider, urlPostedBackToUsByClientAfterUserGaveConsent);

        // Check that user-site is connected.
        assertThat(loginResponseDTO.getUserSite().getConnectionStatus()).isEqualTo(CONNECTED);
    }

    private PostConnectResult postConnect(String provider) {
        // site-management will make a call to providers to retrieve the login information for the specific bank (providers will construct an URL)
        FauxProvidersService.setupPostLoginInfoStubForRedirectStep(wireMockServer, provider, (baseClientRedirectUrl, state) -> "https://abnamro.example.com?redirect_uri=" + baseClientRedirectUrl + "&state=" + state);

        var loginStepResponse = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s",
                        flowContext.user().getId(),
                        flowContext.clientSite().getSiteId(),
                        flowContext.clientSite().getAISRedirectUrlId()
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), LoginStepV1DTO.class);

        // Check the validity of the response.
        assertThat(loginStepResponse.getStatusCode()).isEqualTo(OK);
        var loginStepResponseBody = loginStepResponse.getBody();
        LoginStepV1DTOAssertions.assertIsRedirectStep(loginStepResponseBody);

        return new PostConnectResult(loginStepResponseBody.getUserSiteId(), loginStepResponseBody.getRedirect().getUrl());
    }

    public LoginResponseDTO postUserSite(String provider, String validConsentUrl) {
        // site-management will make a call to providers for the token exchange, we make providers respond with valid AccessMeans
        FauxProvidersService.setupPostAccessMeansCreateStubForAccessMeans(wireMockServer, provider,
                apiCreateAccessMeansDTO -> true,
                // Note: we are returning AccessMeans that are immediately expired (they are valid until -10s ago)
                //       this will cause site-management to refresh the AccessMeans immediately.
                () -> Instant.now(systemUTC()).minus(10, SECONDS)
        );
        // Setup the renew access means response.
        FauxProvidersService.setupRefreshAccessMeansReconsentExpiredSuccess(wireMockServer, provider, () -> Instant.now(clock).plus(90, DAYS));

        // Setup response from providers that site-management will call to trigger a data fetch
        FauxProvidersService.setupPostFetchDataStub(wireMockServer, provider);
        // site-management will ask accounts-and-transactions for the user site transaction summary.
        FauxAccountsAndTransactionsService.setupUserSiteTransactionStatusSummary(wireMockServer);

        boolean refreshCalled = wireMockServer.getServeEvents().getRequests().stream().anyMatch(r -> r.getRequest().getUrl().contains("/access-means/refresh"));
        assertThat(refreshCalled)
                .withFailMessage("/access-means/refresh should not have been called at this state")
                .isFalse();

        var body = new UrlLoginDTO();
        body.setRedirectUrl(validConsentUrl);
        var loginResponse = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(loginResponse.getStatusCode()).isEqualTo(OK);

        refreshCalled = wireMockServer.getServeEvents().getRequests().stream().anyMatch(r -> r.getRequest().getUrl().contains("/access-means/refresh"));
        assertThat(refreshCalled)
                .withFailMessage("/access-means/refresh should have been called")
                .isTrue();

        return loginResponse.getBody();
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
