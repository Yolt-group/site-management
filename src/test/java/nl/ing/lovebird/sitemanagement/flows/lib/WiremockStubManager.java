package nl.ing.lovebird.sitemanagement.flows.lib;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;

import java.util.Map;

public class WiremockStubManager {

    public static Map<String, Boolean> flowStubMetaData = Map.of("add-user-site-flow", true);
    public static Map<String, Boolean> providerSitesStubMetaData = Map.of("sites-details", true);

    public static void clearFlowStubs(WireMockServer wireMockServer) {
        var pattern = new ContainsPattern("add-user-site-flow");
        wireMockServer.removeServeEventsForStubsMatchingMetadata(pattern);
        wireMockServer.removeStubMappingsByMetadata(pattern);

        var sitesDetailsPattern = new ContainsPattern("sites-details");
        wireMockServer.removeServeEventsForStubsMatchingMetadata(sitesDetailsPattern);
        wireMockServer.removeStubMappingsByMetadata(sitesDetailsPattern);
    }

}
