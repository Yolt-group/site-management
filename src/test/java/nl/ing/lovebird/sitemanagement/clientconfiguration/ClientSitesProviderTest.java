package nl.ing.lovebird.sitemanagement.clientconfiguration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;


@IntegrationTestContext
class ClientSitesProviderTest {

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ClientSitesProvider clientSitesProvider;

    @Test
    public void when_AnHttpRequestFails_then_theClientSitesListIsNotUpdated() {

        wireMockServer.stubFor(WireMock.get(urlMatching("/clients/internal/v2/sites-per-client"))
                .willReturn(aResponse()
                        .withBodyFile("clients/client-sites.json")
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        clientSitesProvider.update();
        int siteNumberAfterFirstUpdate = clientSitesProvider.getClientSites(ClientIds.TEST_CLIENT).size();

        wireMockServer.stubFor(WireMock.get(urlMatching("/clients/internal/v2/sites-per-client"))
                .willReturn(aResponse()
                        .withStatus(NOT_FOUND.value())
                ));

        clientSitesProvider.update();
        int siteNumberAfterFailedUpdate = clientSitesProvider.getClientSites(ClientIds.TEST_CLIENT).size();


        assertThat(siteNumberAfterFirstUpdate).isEqualTo(8);
        assertThat(siteNumberAfterFailedUpdate).isEqualTo(8);
    }

}