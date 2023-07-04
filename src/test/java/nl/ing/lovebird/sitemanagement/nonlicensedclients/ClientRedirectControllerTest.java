package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.RedirectStep;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.uuid.AISState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTestContext
class ClientRedirectControllerTest {

    private static final UUID SITE_ID = TestProviderSites.YOLT_TEST_BANK.getId();

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ConsentSessionService userSiteSessionService;

    @Autowired
    private ClientRedirectUrlService clientRedirectUrlService;

    @Autowired
    private MockMvc mockMvc;

    @Value("${yolt.yts-group.not-found-url}")
    private String developerPortalNotFoundUrl;

    @Value("${yolt.yts-group.bad-request-url}")
    private String developerPortalBadRequestUrl;

    @Autowired
    private SitesProvider sitesProvider;

    @BeforeEach
    public void setUp() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer,
                new ProvidersSites(List.of(TestProviderSites.YOLT_TEST_BANK), Collections.emptyList()));
        sitesProvider.update();
    }

    @Test
    @SneakyThrows
    public void given_stateForUserSite_when_userRedirectedToUs_then_redirectUserToClient() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(state, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);

        mockMvc.perform(clientRedirectRequest(state.state()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "?state=" + state.state()));
    }

    @Test
    @SneakyThrows
    public void given_unknownState_when_userRedirectedToUs_then_redirectUserToNotFoundErrorPage() {
        var state = UUID.randomUUID();

        mockMvc.perform(clientRedirectRequest(state))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalNotFoundUrl));
    }

    @Test
    @SneakyThrows
    public void given_redirectUrlWithoutState_when_userRedirectedToUs_then_redirectUserToNotFoundErrorPage() {
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/client-redirect"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("client-redirect", 30))
                .andExpect(cookie().value("client-redirect", notNullValue()))
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThat(body).startsWith("<!DOCTYPE html>");
    }

    @Test
    @SneakyThrows
    public void given_missingRedirectUrl_when_userRedirectedToUs_then_redirectUserToBadRequestErrorPage() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();

        createConsentSession(state, clientId, redirectUrlId);

        mockMvc.perform(clientRedirectRequest(state.state()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalBadRequestUrl));
    }

    @Test
    @SneakyThrows
    public void given_redirectWithAdditionalQueryParam_when_userRedirectedToUs_then_redirectUserToClientWithAdditionalQueryParam() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(state, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);

        mockMvc.perform(clientRedirectRequest(state.state()).queryParam("extra", "1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "?state=" + state.state() + "&extra=1"));
    }

    @Test
    @SneakyThrows
    public void given_stateForUserSite_when_urlIsPostedFromJavascript_then_redirectUserToClient() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(state, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);

        mockMvc.perform(javascriptRedirectRequest(urlWithFragmentState(state.state())))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "#state=" + state.state()));
    }

    @Test
    @SneakyThrows
    public void given_queryStateForUserSite_when_urlIsPostedFromJavascript_then_redirectUserToClient() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(state, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);

        mockMvc.perform(javascriptRedirectRequest(urlWithQueryState(state.state())))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "?state=" + state.state()));
    }

    @Test
    @SneakyThrows
    public void given_unknownState_when_urlIsPostedFromJavascript_then_redirectUserToNotFoundErrorPage() {
        var state = UUID.randomUUID();

        mockMvc.perform(javascriptRedirectRequest(urlWithFragmentState(state)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalNotFoundUrl));
    }

    @Test
    @SneakyThrows
    public void given_redirectUrlWithoutState_when_urlIsPostedFromJavascript_then_submitUrlContainingFragmentWithJavascript() {
        mockMvc.perform(javascriptRedirectRequest("https://client-redirect/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalNotFoundUrl));
    }

    @Test
    @SneakyThrows
    public void given_missingRedirectUrl_when_urlIsPostedFromJavascript_then_redirectUserToBadRequestErrorPage() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();

        createConsentSession(state, clientId, redirectUrlId);

        mockMvc.perform(javascriptRedirectRequest(urlWithFragmentState(state.state())))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalBadRequestUrl));
    }

    @Test
    @SneakyThrows
    public void given_redirectWithAdditionalQueryParam_when_urlIsPostedFromJavascript_then_redirectUserToClientWithAdditionalQueryParam() {
        var state = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(state, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);

        mockMvc.perform(javascriptRedirectRequest("https://client-redirect/?extra=2#state=" + state.state() + "&extra=1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "?extra=2#state=" + state.state() + "&extra=1"));
    }

    @Test
    @SneakyThrows
    public void given_redirectWithQueryAndFragmentState_when_urlIsPostedFromJavascriptAndQueryStateExists_then_redirectUserToClientWithBothStates() {
        var existingState = AISState.random();
        var imaginedState = AISState.random();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(existingState, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);
        mockMvc.perform(javascriptRedirectRequest(urlWithQueryState(existingState.state()) + "#state=" + imaginedState.state()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(baseUrl + "?state=" + existingState.state() + "#state=" + imaginedState.state()));
    }

    @Test
    @SneakyThrows
    public void given_redirectWithQueryAndFragmentState_when_urlIsPostedFromJavascriptAndFragmentStateExists_then_redirectUserToNotFoundErrorPage() {
        var existingState = AISState.random();
        var imaginedState = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var baseUrl = "https://client/redirect";

        createConsentSession(existingState, clientId, redirectUrlId);
        createRedirectUrl(clientId, redirectUrlId, baseUrl);
        mockMvc.perform(javascriptRedirectRequest(urlWithQueryState(imaginedState) + "#state=" + existingState))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(developerPortalNotFoundUrl));
    }

    private void createConsentSession(AISState state, UUID clientId, UUID redirectUrlId) {
        userSiteSessionService.createConsentSession(state, new ClientId(clientId), UUID.randomUUID(), SITE_ID,
                new RedirectStep("", null, null, state.state()), redirectUrlId, null, null);
    }

    private void createRedirectUrl(UUID clientId, UUID redirectUrlId, String url) {
        clientRedirectUrlService.save(new ClientId(clientId), redirectUrlId, url);
    }

    private static MockHttpServletRequestBuilder clientRedirectRequest(UUID state) {
        return MockMvcRequestBuilders.get("/client-redirect")
                .queryParam("state", state.toString());
    }

    @SneakyThrows
    private static MockHttpServletRequestBuilder javascriptRedirectRequest(String url) {
        return MockMvcRequestBuilders.post("/client-redirect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("url", url);
    }

    private static String urlWithQueryState(UUID state) {
        return "https://client-redirect/?state=" + state;
    }

    private static String urlWithFragmentState(UUID state) {
        return "https://client-redirect/#state=" + state;
    }
}
