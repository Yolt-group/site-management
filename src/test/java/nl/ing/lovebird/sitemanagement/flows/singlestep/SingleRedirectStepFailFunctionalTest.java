package nl.ing.lovebird.sitemanagement.flows.singlestep;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSitesProvider;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.exception.MissingStateException;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.OnboardedSiteContext;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.lang.String.format;
import static nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService.setupPostLoginInfoStubForRedirectStep;
import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.DISCONNECTED;
import static nl.ing.lovebird.sitemanagement.usersite.FailureReason.AUTHENTICATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.post;

/**
 * This test covers all places where site-management can encounter a functional error [1].  For the sake of brevity
 * we only deal with functional errors that can occur during a flow where a client "acts correctly" and doesn't send us
 * bogus identifiers or urls etc.  Perhaps we can add these tests later.  An example of a case like this is a client
 * posting an URL to us that doesn't contain a state query parameter.  This will result in a {@link MissingStateException}
 * that is mapped to a 400 Bad Request.  This flow is not interesting enough to test since it doesn't change any state
 * on the server side.
 * <p>
 * Scenarios where a **functional** error can occur (restricting ourselves to API Connections):
 * 1. site-management considers the redirect url valid but providers cannot exchange the auth code parameter for a token with the bank
 * <p>
 * [1] a functional error is anything non-technical, think: user clicked access denied, etc.
 */
@IntegrationTestContext
public class SingleRedirectStepFailFunctionalTest {

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
    public void given_validRedirectUrl_when_postUserSite_then_creationOfAccessMeansFails() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();

        // A call to ProviderRestClient.createNewAccessMeans() will fail
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/ABN_AMRO/access-means/create\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        // site-management will make a call to providers to retrieve the login information for the specific bank (providers will construct an URL)
        setupPostLoginInfoStubForRedirectStep(wireMockServer,
                flowContext.clientSite().getSite().getProvider(),
                (baseClientRedirectUrl, state) -> "https://abnamro.example.com?redirect_uri=" + baseClientRedirectUrl + "&state=" + state);

        var loginStepResponse = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s", flowContext.user().getId(), flowContext.clientSite().getSiteId(), flowContext.clientSite().getAISRedirectUrlId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), LoginStepV1DTO.class);
        assertThat(loginStepResponse.getStatusCode()).isEqualTo(OK);

        var urlLogin = new UrlLoginDTO();
        urlLogin.setRedirectUrl(flowContext.clientSite().getRedirectBaseUrlAIS() + "?state=" + parse(loginStepResponse.getBody().getRedirect().getUrl()).getState());
        var loginResponse = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(urlLogin), LoginResponseDTO.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(OK);
        assertThat(loginResponse.getBody().getUserSite().getConnectionStatus()).isEqualTo(DISCONNECTED);
        assertThat(loginResponse.getBody().getUserSite().getLastDataFetchFailureReason()).isEqualTo(AUTHENTICATION_FAILED);
    }

    @Test
    public void given_notCompletedRedirectStep_when_stateIsSubmittedWithForm_then_noUserSiteIsCreated() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer);
        sitesProvider.update();
        flowContext.initialize(sitesProvider, kafkaTemplate, "ABN_AMRO", clientSitesProvider, wireMockServer, clientRedirectUrlService, testClientTokens);

        FauxProvidersService.setupPostLoginInfoStubForRedirectStep(wireMockServer, "ABN_AMRO", (b, s) -> "b?state=s");

        ResponseEntity<LoginStepV1DTO> connectResp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s",
                        flowContext.user().getId(),
                        flowContext.clientSite().getSiteId(),
                        flowContext.clientSite().getAISRedirectUrlId()
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), LoginStepV1DTO.class);

        FormLoginDTO body = new FormLoginDTO();
        body.setStateId(connectResp.getBody().getRedirect().getState());
        body.setFilledInFormValues(Collections.emptyList());

        ResponseEntity<ErrorDTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), ErrorDTO.class);

        assertThat(restTemplate.exchange(get(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                .headers(flowContext.httpHeadersForClientAndUser())
                .header("PSU-IP-Address", flowContext.user().getPsuIpAddress()).build(), new ParameterizedTypeReference<List<UserSiteDTO>>() {
        }).getBody()).hasSize(0);
        assertThat(resp.getStatusCode().isError()).isTrue();
    }

    @AfterEach
    public void tearDown() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

}
