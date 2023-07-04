package nl.ing.lovebird.sitemanagement.flows.multistep;

import lombok.SneakyThrows;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.forms.SelectFieldDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.post;

/**
 * This flow is an extension of the {@link MultiStepSuccessTest}. This flow cleans-up dangling user-site-sessions which will result in a failure of the flow..
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
 * actors: site-management
 * code: {@link #cleanupUserSite()}
 * -> cleaning up the user-sites would normally be a job that runs every minute and invalidates user-sites for which the
 * user-site-session exists for a certain period. In this case we use the threshold of 0 seconds which effectively will
 * clean-up all user-sites.
 * <p>
 * Step 5
 * actors: site-management
 * code: {@link #getNextStep(UUID)}
 * -> getting the next step will fail as the user-site is cleaned-up in the previous state.
 */
@IntegrationTestContext
public class MultiStepTimeoutTest extends MultiStepBaseTest {

    @AfterEach
    public void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    @SneakyThrows
    public void given_onboardedClientAndSiteWithMultistepFlow_when_sessionTimesOut_then_flowFails() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        flowContext.initialize(sitesProvider, kafkaTemplate, "CREDITAGRICOLE", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        // Convenience variable: the provider is used in various places.
        var provider = flowContext.clientSite().getSite().getProvider();

        // Step 1
        var postConnectResult = postConnect(provider);

        // We are expecting 1 FormField: a region selection field.
        assertThat(postConnectResult.getForm().getFormComponents().size()).isEqualTo(1);
        SelectFieldDTO regionSelectionField = (SelectFieldDTO) postConnectResult.getForm().getFormComponents().get(0);

        // Step 2
        // Fill in the form: simply select the 1st option.
        var regionFieldId = regionSelectionField.getId();
        var selectedValue = regionSelectionField.getSelectOptionValues().get(0).getValue();

        // Step 3
        var postFormResult = postRegionForm(provider, postConnectResult.getForm().getStateId(), Map.of(regionFieldId, selectedValue));

        // Step 4
        cleanupUserSite();

        // Step 5
        getNextStep(postFormResult.getUserSiteId());
    }

    private void getNextStep(UUID userSiteId) {
        var resp = restTemplate.exchange(
                get(URI.create(format("/v1/users/%s/user-sites/%s/step",
                        flowContext.user().getId(),
                        userSiteId
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), ErrorDTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(resp.getBody().getCode()).isEqualTo("SM042");
    }

    private void cleanupUserSite() {
        var resp = restTemplate.exchange(
                post(URI.create("/v1/usersites/loginstep-cleanup?ttl=0"))
                        .headers(flowContext.httpHeadersForClientAndUser()).build(),
                Void.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(ACCEPTED);
    }
}
