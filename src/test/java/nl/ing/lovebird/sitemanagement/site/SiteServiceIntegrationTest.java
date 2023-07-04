package nl.ing.lovebird.sitemanagement.site;

import com.datastax.driver.core.Session;
import com.github.tomakehurst.wiremock.WireMockServer;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.exception.SiteNotFoundException;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;

import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.sites.RegisteredSite;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import org.assertj.core.api.Java6Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static nl.ing.lovebird.providerdomain.ServiceType.AIS;
import static nl.ing.lovebird.providerdomain.ServiceType.PIS;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestContext
public class SiteServiceIntegrationTest {

    private static final UUID EXPERIMENTAL_SITE_ID = UUID.fromString("36130c5f-9024-4a89-91fc-be31fac2f9ec"); // Matches experimental site id from properties
    private static final String EXPERIMENTAL_SITE_NAME = "Experimental site name";
    private static final String EXPERIMENTAL_SITE_PROVIDER_KEY = "TEST_IMPL_OPENBANKING"; // TODO C4PO-7504 limited by Provider.java enum, should be set to random value after Provider.java is removed
    private static final UUID TEST_SITE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String TEST_SITE_NAME = "Test site name";
    private static final String TEST_SITE_PROVIDER_KEY = "YOLT_PROVIDER"; // TODO C4PO-7504 limited by Provider.java enum, should be set to random value after Provider.java is removed
    private static final UUID SITE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String SITE_NAME = "Site name";
    private static final String SITE_PROVIDER_KEY = "BARCLAYS"; // TODO C4PO-7504 limited by Provider.java enum, should be set to random value after Provider.java is removed
    private static final UUID SCRAPING_SITE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String SCRAPING_SITE_NAME = "Site name";
    private static final String SCRAPING_SITE_PROVIDER_KEY = "YODLEE"; // TODO C4PO-7504 limited by Provider.java enum, should be set to random value after Provider.java is removed

    @Autowired
    private SiteService siteService;

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SitesProvider sitesProvider;

    @Autowired
    private Session session;

    @BeforeEach
    void setUp() {
        FauxProvidersService.setupProviderSitesStub(wireMockServer, new ProvidersSites(List.of(
                createExampleSite(TEST_SITE_ID, TEST_SITE_NAME, TEST_SITE_PROVIDER_KEY),
                createExampleSite(EXPERIMENTAL_SITE_ID, EXPERIMENTAL_SITE_NAME, EXPERIMENTAL_SITE_PROVIDER_KEY),
                createExampleSite(SITE_ID, SITE_NAME, SITE_PROVIDER_KEY),
                createExampleSite(SCRAPING_SITE_ID, SCRAPING_SITE_NAME, SCRAPING_SITE_PROVIDER_KEY)
        ), Collections.emptyList()));
        sitesProvider.update();
    }

    @AfterEach
    public void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    void when_IRequestHSBCSite_then_itIsReturned() {
        // Just a random 'non-test' site example.
        final Site response = siteService.getSite(SITE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(SITE_NAME);
    }

    @Test
    void when_IRequestYoltProviderSiteTestSite_then_itIsReturned() {

        final Site response = siteService.getSite(TEST_SITE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(TEST_SITE_NAME);
    }

    @Test
    void when_ICheckIfAYoltbankSiteIsStubbed_then_IGetAProperResponse() {

        // real yodlee. (testbank stubbed by yodlee)
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("e278a008-bf45-4d19-bb5d-b36ff755be58"))).isFalse();
        // real bi. (testbank stubbed by bi)
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("93bac548-d2de-4546-b106-880a5018460d"))).isFalse();
        // saltedge. Partly stubbed by yoltbank
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("082af309-8f16-452c-a48e-0a8830b603b7"))).isTrue();
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("33e51254-d12a-4380-aed6-f5f912d8da9f"))).isFalse();
        // BI yoltbank
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("0285a320-7dca-11e8-adc0-fa7ae01bbebc"))).isTrue();
        // yodlee yoltbank
        assertThat(SiteService.isSiteStubbedByYoltbank(UUID.fromString("8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc"))).isTrue();
    }

    @Test
    void when_ITryToGetASiteThatDoesntExist_then_IGetASiteNotFoundException() {
        assertThatThrownBy(() -> siteService.getSite(UUID.randomUUID())).isInstanceOf(SiteNotFoundException.class);
    }

    @Test
    void when_IGetAllSites_then_theyShouldALlHaveAvailableInCountriesProperty() {
        List<String> sitesWithoutAvailableInCountries = siteService.getSites().stream()
                .map(it -> {
                    if (it.getAvailableInCountries().isEmpty()) {
                        return it.getId() + ", " + it.getName() + " has no availableInCountries!";
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Java6Assertions.assertThat(sitesWithoutAvailableInCountries).isNullOrEmpty();
    }

    @Test
    void when_IGetAllSites_then_TheyShouldHaveServiceTypesLoginRequirementsAndServicesAndWhiteList() {
        List<String> siteErrorMessages = siteService.getSites().stream()
                .map(it -> {
                    // Check if services are specified.
                    if (it.getServices() == null || it.getServices().isEmpty()) {
                        return it.getId() + ", " + it.getName() + " has no services!";
                    }

                    // Check if for those services the loginRequirements are set.
                    StringBuilder s = new StringBuilder();
                    it.getServices().forEach(serviceType -> {
                        List<LoginRequirement> loginRequirements = it.getUsesStepTypes().get(serviceType);
                        if (loginRequirements == null || loginRequirements.isEmpty()) {
                            s.append(it.getId()).append(", ").append(it.getName()).append(" has no loginRequirement for service ").append(serviceType).append("!");
                            s.append(System.lineSeparator());
                        }
                    });
                    // Check if accountWhitelist is specified

                    if (it.getServices().contains(ServiceType.AIS) && it.getAccountTypeWhitelist().isEmpty()) {
                        return it.getId() + ", " + it.getName() + " has no whitelisted accounttypes!";
                    }
                    String errors = s.toString();
                    if (isNotBlank(errors)) {
                        return errors;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        Java6Assertions.assertThat(siteErrorMessages).isNullOrEmpty();
    }

    @Test
    void when_IGetAllSitesWithingAGroup_theGroupShouldBeValidForMigration() {
        final Map<String, List<Site>> groupedSites = siteService.getSites()
                .stream()
                .filter(site -> site.getGroupingBy() != null)
                .collect(Collectors.groupingBy(Site::getGroupingBy));

        Java6Assertions.assertThat(groupedSites).isNotEmpty();

        final Map<String, List<Site>> faultyGroupsMap = groupedSites.entrySet().stream()
                .filter(e -> !isValidYodleeGroup(e.getKey(), e.getValue()))
                // This exemption is necessary because we're introducing an unnecessary migration scenario for the client for France, namely: "scraper [curr,save,cred] -> scraper [save,cred]"
                .filter(e -> e.getValue().stream().noneMatch(s -> "Scen. 5: FR Bank Migration Partial migration (noLongerSupported)".equals(s.getGroupingBy())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(faultyGroupsMap).isEmpty();
    }

    private boolean isValidYodleeGroup(final String group, final List<Site> sites) {
        // This is copied, not mine. I think the requirements of this test are a bit vague....
        // Previously it checked that: we had 1 yodlee with CUR/CRED/SAV , 1 yodlee with CRED/SAV, 1 OB with CURR. This doesn't really apply
        // anymore, so we'll just change it to make sure that there is at least a direct connection type in the group.
        // There could be overlap in whitelisted accounttypes now..
        final UUID oldYodlee = sites.stream().filter(site -> site.getAccountTypeWhitelist().size() == 3).findFirst().map(Site::getId).orElse(null);
        final UUID ob = sites.stream().filter(site -> site.getAccountTypeWhitelist().size() == 1).findFirst().map(Site::getId).orElse(null);
        final UUID newYodlee = sites.stream().filter(site -> site.getAccountTypeWhitelist().size() == 2).findFirst().map(Site::getId).orElse(null);
        System.out.println(String.format("%s, %s, %s, group: %s", oldYodlee, ob, newYodlee, group));

        if (sites.size() > 1) {
            if (arePairOfGroupOfSitesScraperTypeOnly(sites)) {
                return true;
            }
            final boolean hasDifferentProviders = sites.stream().map(Site::getProvider).distinct().count() > 1;
            return hasDifferentProviders;
        }
        return true;
    }

    private boolean arePairOfGroupOfSitesScraperTypeOnly(List<Site> sites) {
        return sites.size() == 2
                && sites.stream().allMatch(it -> isScrapingSite(it.getProvider()));
    }

    private RegisteredSite createExampleSite(UUID id, String name, String providerKey) {
        return new RegisteredSite(
                name,
                providerKey,
                "groupingBy" + name,
                id,
                List.of(AccountType.CURRENT_ACCOUNT),
                List.of(CountryCode.GB),
                90,
                null,
                null,
                Map.of(AIS, List.of(LoginRequirement.REDIRECT), PIS, List.of(LoginRequirement.REDIRECT))
        );
    }
}
