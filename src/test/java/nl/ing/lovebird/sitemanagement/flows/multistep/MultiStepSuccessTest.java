package nl.ing.lovebird.sitemanagement.flows.multistep;

import lombok.SneakyThrows;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.*;
import nl.ing.lovebird.sitemanagement.flows.singlestep.SingleRedirectStepSuccessTest;
import nl.ing.lovebird.sitemanagement.forms.SelectFieldDTO;
import nl.ing.lovebird.sitemanagement.usersite.LoginResponseDTO;
import nl.ing.lovebird.sitemanagement.usersite.UrlLoginDTO;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteDTO;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
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
 * This flow is an extension of the {@link SingleRedirectStepSuccessTest}.  This flow adds a FormStep to the flow.
 * <p>
 * In particular, these are the steps:
 * <p>
 * Step 1
 * actors: client, site-management, providers pod
 * code: {@link #postConnect}
 * -> client retrieves the first step from site-management which is a FormStep that requires the user to select a region
 * <p>
 * Step 2
 * actors: user
 * code: n/a
 * -> The user selects the region and submits the form back to the client
 * <p>
 * Step 3
 * actors: client, site-management, providers pod
 * code: {@link #postRegionForm}
 * -> client posts form back to site-management, we propagate to providers, we receive another step back (redirect)
 * <p>
 * Step 4
 * actors: client, user, bank
 * code: n/a
 * -> The client redirects the user to the bank where the user performs the steps necessary and is redirect back to the client by the bank.
 * <p>
 * Step 5
 * actors: client, site-management, providers pod, bank
 * code {@link #postUserSite}
 * -> client posts url to which the user was redirected by the bank to site-management
 * <p>
 * Step 6
 * actors: providers pod, accounts-and-transactions pod, bank
 * code: {@link #performDataFetch}
 * -> The system performs the data fetch, it retrieves data from the bank, stores it in the db, and sends kafka messages.
 * <p>
 * Step 7
 * actors: client, site-management
 * code: {@link #getUserSite}
 * -> poll site-management until all the fields have their expected values [1]
 * <p>
 * [1] In practice a client will listen to a webhook that we send out, but that's out of scope for this test, polling
 * the endpoint works fine for our purposes.
 */
@IntegrationTestContext
public class MultiStepSuccessTest extends MultiStepBaseTest {


    @AfterEach
    public void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    @SneakyThrows
    public void given_onboardedClientAndSiteWithMultistepFlow_when_userAddsBank_then_flowIsSuccessful() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        // Credit agricole is one of the banks that features a region selection flow
        flowContext.initialize(sitesProvider, kafkaTemplate, "CREDITAGRICOLE", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        // Convenience variable: the provider is used in various places.
        final String provider = flowContext.clientSite().getSite().getProvider();

        // Step 1
        var postConnectResult = postConnect(provider);

        // We are expecting 1 FormField: a region selection field.
        assertThat(postConnectResult.getForm().getFormComponents().size()).isEqualTo(1);
        SelectFieldDTO regionSelectionField = (SelectFieldDTO) postConnectResult.getForm().getFormComponents().get(0);

        // Step 2
        // Fill in the form: simply select the 1st option.
        String regionFieldId = regionSelectionField.getId();
        String selectedValue = regionSelectionField.getSelectOptionValues().get(0).getValue();

        // Step 3
        var postFormResult = postRegionForm(provider, postConnectResult.getForm().getStateId(), Map.of(regionFieldId, selectedValue));

        // Step 4
        // At this point the client will redirect the user to the url that we provided the client [1].  That url contains
        // a query parameter "state".  The client will give their consent at the bank, the bank will subsequently redirect
        // the user to a webpage owned by the client.  The client will then post the full url [2] to which the user was
        // redirected back to us, this url will contain the same state parameter.  We construct this url here.
        // [1] postConnectResult.redirectUrlFromProviders
        // [2] for the Yolt App such an url might look like "https://www.yolt.com/callback?state=92484886-A556-4A87-978A-AE02E453085B"
        String urlPostedBackToUsByClientAfterUserGaveConsent = flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(postFormResult.getStep().getRedirect().getUrl()).getState();

        // Step 5
        var postRedirectResult = postUserSite(provider, urlPostedBackToUsByClientAfterUserGaveConsent);

        // Step 6
        performDataFetch(provider, postRedirectResult.userSiteId, postRedirectResult.activityId, postRedirectResult.providerRequestId);

        // Step 7
        getUserSite(postRedirectResult);
    }

    private PostUserSiteResult postUserSite(String provider, String validConsentUrl) {
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
        FauxProvidersService.sendKafkaFetchDataSuccessMessage(kafkaTemplate, providerRequestId,  flowContext.getClientUserToken());

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
