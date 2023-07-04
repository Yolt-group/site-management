package nl.ing.lovebird.sitemanagement.flows.singlestep;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
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
import nl.ing.lovebird.sitemanagement.usersite.UserSiteDTO;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.lang.String.format;
import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.post;

/**
 * This flow is the most widely used flow to connect a user-site.  It is also the easiest flow to understand.
 * <p>
 * Step 1
 * actors: client, site-management, providers pod
 * code: {@link #postConnect}
 * -> client retrieves the url of the bank to which the user needs to be redirected
 * <p>
 * Step 2
 * actors: client, user, bank
 * code: n/a
 * -> The user performs the steps necessary at the bank and is redirect back to the client by the bank.
 * <p>
 * Step 3
 * actors: client, site-management, providers pod, bank
 * code {@link #postUserSite}
 * -> client posts url to which the user was redirected by the bank to site-management
 * <p>
 * Step 4
 * actors: providers pod, accounts-and-transactions pod, bank
 * code: {@link #performDataFetch}
 * -> The system performs the data fetch, it retrieves data from the bank, stores it in the db, and sends kafka messages.
 * <p>
 * Step 5
 * actors: client, site-management
 * code: {@link #getUserSite}
 * -> poll site-management until all the fields have their expected values [1]
 * <p>
 * [1] In practice a client will listen to a webhook that we send out, but that's out of scope for this test, polling
 * the endpoint works fine for our purposes.
 */
@IntegrationTestContext
public class SingleRedirectStepSuccessTest {

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
    @SneakyThrows
    public void given_onboardedClientAndSite_when_userAddsBank_then_flowIsSuccessful() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        // Convenience variable: the provider is used in various places.
        final String provider = flowContext.clientSite().getSite().getProvider();

        // Step 1
        var postConnectResult = postConnect(provider);

        // Step 2
        // At this point the client will redirect the user to the url that we provided the client [1].  That url contains
        // a query parameter "state".  The client will give their consent at the bank, the bank will subsequently redirect
        // the user to a webpage owned by the client.  The client will then post the full url [2] to which the user was
        // redirected back to us, this url will contain the same state parameter.  We construct this url here.
        // [1] postConnectResult.redirectUrlFromProviders
        // [2] for the Yolt App such an url might look like "https://www.yolt.com/callback?state=92484886-A556-4A87-978A-AE02E453085B"
        String urlPostedBackToUsByClientAfterUserGaveConsent = flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(postConnectResult.redirectUrlFromProviders).getState();

        // Step 3
        var postUserSiteResult = postUserSite(provider, urlPostedBackToUsByClientAfterUserGaveConsent);

        // Perform a sanity check, the method postConnect has 'reserved' a UserSite.id for us, we will validate that it matches
        // the UserSite.id returned after successfully connecting the usersite.
        assertThat(postUserSiteResult.userSiteId)
                .withFailMessage("Expecting the UserSite.id that was reserved by POST /connect to be equal to the one assigned the UserSite created by POST /user-sites")
                .isEqualTo(postConnectResult.userSiteId);

        // Step 4
        // Simulate the system (providers, accounts-and-transactions) performing the data fetch in the background.
        performDataFetch(provider, postUserSiteResult.userSiteId, postUserSiteResult.activityId, postUserSiteResult.providerRequestId);

        // Step 5
        // Poll the user-sites endpoint until the returned object represents a user-site that has been successfully
        // connected and for which data has been successfully fetched.
        getUserSite(postUserSiteResult);
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

    public PostUserSiteResult postUserSite(String provider, String validConsentUrl) {
        // site-management will make a call to providers for the token exchange, we make providers respond with valid AccessMeans
        FauxProvidersService.setupPostAccessMeansCreateStubForAccessMeans(wireMockServer, provider,
                apiCreateAccessMeansDTO -> true,
                () -> Instant.now(systemUTC()).plus(90, DAYS)
        );
        // site-management will make a call to providers to trigger a data fetch
        FauxProvidersService.setupPostFetchDataStub(wireMockServer, provider);
        // site-management will ask accounts-and-transactions for the user site transaction summary.
        FauxAccountsAndTransactionsService.setupUserSiteTransactionStatusSummary(wireMockServer);

        UrlLoginDTO body = new UrlLoginDTO();
        body.setRedirectUrl(validConsentUrl);
        ResponseEntity<LoginResponseDTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginResponseDTO dto = resp.getBody();
        LoginResponseDTOAssertions.assertConnectedJustNow(dto, Instant.now(clock));

        // Find the providerRequestId
        UUID providerRequestId = FauxProvidersService.extractProviderRequestIdFromServedRequests(wireMockServer, provider);

        // Ensure we are connected to the site we asked for.
        assertThat(dto.getUserSite().getSite().getId())
                .withFailMessage("We were connected to the wrong site.  Should never happen.")
                .isEqualTo(flowContext.clientSite().getSiteId());

        return new PostUserSiteResult(dto.getUserSiteId(), dto.getActivityId(), providerRequestId);
    }

    /**
     * Simulate the system completing a data fetch.
     */
    private void performDataFetch(String provider, UUID userSiteId, UUID activityId, UUID providerRequestId) {
        // The previous step resulted in a connected user-site.  In practice a data-fetch has been started in the background,
        // site-management will be updated asynchronously over Kafka by providers.  Simulate this to validate the UserSite
        // object after a data fetch has successfully completed.
        //
        // Note: this message will have no effect in the happy flow scenario, we include it for completeness, some day it
        //       might have an effect ;-)
        FauxProvidersService.sendKafkaFetchDataSuccessMessage(kafkaTemplate, providerRequestId, flowContext.getClientUserToken());

        // The attribute lastDataFetchTime of a UserSite is updated when site-management receives an IngestionFinished event,
        // this event is sent by the accounts-and-transactions pod after it has finished processing the data sent to it by the
        // providers service.  In effect, triggering the data fetch at providers results in 2 messages arriving at site-management
        // 1) directly from providers informing us about the result of the data fetch operation (the message above)
        // 2) indirectly from accounts-and-transactions informing us which accounts were updated, etc.
        FauxAccountsAndTransactionsService.sendKafkaIngestionFinishedEvent(kafkaTemplate, activityId, provider, userSiteId, flowContext.getClientUserToken());
    }

    private void getUserSite(PostUserSiteResult postUserSiteResult) {
        await().atMost(Durations.TEN_SECONDS).until(() -> {
            ResponseEntity<UserSiteDTO> resp = restTemplate.exchange(
                    get(URI.create(format("/v1/users/%s/user-sites/%s", flowContext.user().getId(), postUserSiteResult.getUserSiteId())))
                            .headers(flowContext.httpHeadersForClientAndUser())
                            .build(), UserSiteDTO.class);

            if (resp.getBody() == null || resp.getBody().getLastDataFetchTime() == null) {
                // Try again
                return false;
            }

            assertThat(resp.getStatusCode()).isEqualTo(OK);
            UserSiteDTO dto = resp.getBody();
            UserSiteDTOAssertions.assertDataFetchCompletedJustNow(dto);

            // Check values (similar checks to what we just did after connecting the user site, see above).
            assertThat(dto.getId()).isEqualTo(postUserSiteResult.getUserSiteId());
            // Check that the site matches the site we requested.
            assertThat(dto.getSite().getId()).isEqualTo(flowContext.clientSite().getSiteId());
            return true;
        });
    }

    @Value
    private static class PostConnectResult {

        UUID userSiteId;
        String redirectUrlFromProviders;
    }

    @Value
    private static class PostUserSiteResult {

        UUID userSiteId;
        UUID activityId;
        UUID providerRequestId;
    }

    @AfterEach
    public void tearDown() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }
}
