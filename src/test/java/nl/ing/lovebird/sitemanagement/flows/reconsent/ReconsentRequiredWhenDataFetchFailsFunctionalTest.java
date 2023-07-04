package nl.ing.lovebird.sitemanagement.flows.reconsent;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.Value;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSitesProvider;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.*;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.*;
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
import static java.time.temporal.ChronoUnit.SECONDS;
import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.DISCONNECTED;
import static nl.ing.lovebird.sitemanagement.usersite.FailureReason.AUTHENTICATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.post;

/**
 * Given a connected user site, test that whenever a bank informs us during a data fetch that the token is no longer
 * valid for the usersite, that we update the usersite status to {@link ConnectionStatus#DISCONNECTED} and the
 * lastDataFetchFailureReason to {@link FailureReason#CONSENT_EXPIRED}
 */
@IntegrationTestContext
public class ReconsentRequiredWhenDataFetchFailsFunctionalTest {

    @Autowired
    private Clock clock;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    private SitesProvider sitesProvider;
    @Autowired
    private ClientSitesProvider clientSitesProvider;
    @Autowired
    private ClientRedirectUrlService clientRedirectUrlService;
    @Autowired
    private TestClientTokens testClientTokens;
    OnboardedSiteContext flowContext = new OnboardedSiteContext();

    @Test
    public void given_consentExpiresSoon_when_triggeringDataFetch_then_reconsentRequired() {
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

        // Check that auth failed (access means expired (or close to))
        assertThat(loginResponseDTO.getUserSite().getConnectionStatus()).isEqualTo(DISCONNECTED);
        assertThat(loginResponseDTO.getUserSite().getLastDataFetchFailureReason()).isEqualTo(AUTHENTICATION_FAILED);

        // Ensure we are connected to the site we asked for.
        assertThat(loginResponseDTO.getUserSite().getSite().getId())
                .withFailMessage("We were connected to the wrong site.  Should never happen.")
                .isEqualTo(flowContext.clientSite().getSiteId());

        // Perform a sanity check, the method postConnect has 'reserved' a UserSite.id for us, we will validate that it matches
        // the UserSite.id returned after successfully connecting the usersite.
        assertThat(loginResponseDTO.getUserSiteId())
                .withFailMessage("Expecting the UserSite.id that was reserved by POST /connect to be equal to the one assigned the UserSite created by POST /user-sites")
                .isEqualTo(postConnectResult.userSiteId);
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
                () -> Instant.now(systemUTC()).plus(10, SECONDS)
        );
        // Setup response from providers that site-management will call to trigger a data fetch
        FauxProvidersService.setupPostFetchDataStub(wireMockServer, provider);
        // site-management will ask accounts-and-transactions for the user site transaction summary.
        FauxAccountsAndTransactionsService.setupUserSiteTransactionStatusSummary(wireMockServer);

        // Re-consent needed for access-means.
        FauxProvidersService.setupRefreshAccessMeansReconsentExpiredFailure(wireMockServer, provider);

        var body = new UrlLoginDTO();
        body.setRedirectUrl(validConsentUrl);
        var loginResponse = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(loginResponse.getStatusCode()).isEqualTo(OK);

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
