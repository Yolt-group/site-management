package nl.ing.lovebird.sitemanagement.sites;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTestContext
class SitesProviderTest {

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    public void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    public void update_sitesAreNoteUpdatedWhenDuplicatesAreReturned() throws JsonProcessingException {

        List<Site> initialSites = sitesProvider.allSites();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleDuplicatedProvidersSites(2)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        // when
        sitesProvider.update();

        // then
        assertThat(sitesProvider.allSites()).isEqualTo(initialSites);
    }

    @Test
    public void allSites_allSitesAreAvailableAfterProvidersFail() throws JsonProcessingException {

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleProvidersSites(1)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();
        int siteNumberAfterFirstUpdate = sitesProvider.allSites().size();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(createExampleProvidersSites(2)))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));

        sitesProvider.update();
        int siteNumberAfterSecondUpdate = sitesProvider.allSites().size();

        wireMockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .willReturn(aResponse()
                        .withStatus(NOT_FOUND.value())
                ));

        sitesProvider.update();
        int siteNumberAfterFailure = sitesProvider.allSites().size();

        assertThat(siteNumberAfterFirstUpdate).isEqualTo(1);
        assertThat(siteNumberAfterSecondUpdate).isEqualTo(2);
        assertThat(siteNumberAfterFailure).isEqualTo(2);
    }

    private ProvidersSites createExampleProvidersSites(int numberOfSites) {
        List<RegisteredSite> sites = new ArrayList<>();
        for (int i = 0; i < numberOfSites; i++) {
            sites.add(new RegisteredSite("name",
                    "BARCLAYS",
                    "groupingBy",
                    UUID.randomUUID(),
                    List.of(AccountType.CURRENT_ACCOUNT),
                    List.of(CountryCode.PL),
                    90,
                    Set.of(ConsentBehavior.CONSENT_PER_ACCOUNT),
                    "externalId",
                    Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)))
            );
        }
        return new ProvidersSites(sites, Collections.emptyList());
    }

    private ProvidersSites createExampleDuplicatedProvidersSites(int numberOfSites) {
        List<RegisteredSite> sites = new ArrayList<>();
        UUID id = UUID.randomUUID();
        for (int i = 0; i < numberOfSites; i++) {
            sites.add(TestProviderSites.ABN_AMRO);
        }
        return new ProvidersSites(sites, Collections.emptyList());
    }
}
