package nl.ing.lovebird.sitemanagement.site;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteService {

    public static final UUID ID_YOLTBANK_YOLT_PROVIDER = UUID.fromString("33aca8b9-281a-4259-8492-1b37706af6db");
    private static final UUID ID_YOLTBANK_YODLEE_STUBBED = UUID.fromString("8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc");
    private static final UUID ID_YOLTBANK_BI_STUBBED = UUID.fromString("0285a320-7dca-11e8-adc0-fa7ae01bbebc");
    public static final UUID ID_YOLTBANK_OPENBANKING_STUBBED = UUID.fromString("ca8a362a-a351-4358-9f1c-a8b4b91ed65b");
    private static final UUID ID_YOLTBANK_PERF_TEST_BANK_STUBBED = UUID.fromString("c3162db0-62f6-4623-997c-15fa970a082c");
    private static final UUID ID_SALTEDGE_DEMO_BANK_STUBBED = UUID.fromString("082af309-8f16-452c-a48e-0a8830b603b7");
    private static final UUID ID_CESKA_TEST_BANK = UUID.fromString("45ea79d9-e1fc-4a61-812d-5672e970606f");
    private static final UUID ID_POLISH_API_MOCK_BANK = UUID.fromString("ed1fd770-db06-4e93-9199-abccceae3820");
    private static final UUID ID_ING_NL_MOCK_BANK = UUID.fromString("828e4f90-2773-45c2-9199-cbf9264ef1cc");
    private static final UUID ID_FR_MIGRATION_SCEN1_DIRECT_CONNECTION = UUID.fromString("333e1b97-1055-4b86-a112-bc1db801145f");
    private static final UUID ID_FR_MIGRATION_SCEN2_DIRECT_CONNECTION = UUID.fromString("840d4df3-07d2-4d2e-b177-6db8f4cea479");
    private static final UUID ID_FR_MIGRATION_SCEN3_DIRECT_CONNECTION = UUID.fromString("035ba2f1-f751-4d71-be88-3e6649ad1051");
    private static final UUID ID_FR_MIGRATION_SCEN1 = UUID.fromString("21170e28-fe88-465c-8bff-9f6288416b76");
    private static final UUID ID_FR_MIGRATION_SCEN2_START = UUID.fromString("de337ce7-dc43-4971-b0c1-b3ca00b2118c");
    private static final UUID ID_FR_MIGRATION_SCEN2_REMAINDER = UUID.fromString("acb1c151-5a58-4fa5-bb8b-7830c519678a");
    private static final UUID ID_FR_MIGRATION_SCEN3 = UUID.fromString("a10de3d4-93f4-4346-8391-42a8319852b2");
    private static final UUID ID_FR_MIGRATION_SCEN4 = UUID.fromString("7bdbe19f-c564-4dd9-bb89-9873a3fb2037");
    private static final UUID ID_FR_MIGRATION_SCEN5 = UUID.fromString("106a46ec-05b1-4a32-a9fb-ce728c47ce1f");
    private static final UUID ID_FR_MIGRATION_SCEN5_REMAINDER = UUID.fromString("fedcc4d1-3e22-4d79-8947-d8e363622533");

    private static final List<UUID> YOLT_BANK_TEST_SITES_IDS = List.of(ID_YOLTBANK_YOLT_PROVIDER, ID_FR_MIGRATION_SCEN1_DIRECT_CONNECTION, ID_FR_MIGRATION_SCEN2_DIRECT_CONNECTION, ID_FR_MIGRATION_SCEN3_DIRECT_CONNECTION,
            ID_YOLTBANK_YODLEE_STUBBED, ID_YOLTBANK_BI_STUBBED, ID_SALTEDGE_DEMO_BANK_STUBBED, ID_YOLTBANK_OPENBANKING_STUBBED, ID_CESKA_TEST_BANK, ID_POLISH_API_MOCK_BANK,
            ID_ING_NL_MOCK_BANK, ID_YOLTBANK_PERF_TEST_BANK_STUBBED, ID_FR_MIGRATION_SCEN1, ID_FR_MIGRATION_SCEN2_START, ID_FR_MIGRATION_SCEN2_REMAINDER, ID_FR_MIGRATION_SCEN3, ID_FR_MIGRATION_SCEN4,
            ID_FR_MIGRATION_SCEN5, ID_FR_MIGRATION_SCEN5_REMAINDER);

    private final SitesProvider sitesProvider;

    public Site getSite(final UUID siteId) {
        return sitesProvider.findByIdOrThrow(siteId);
    }

    public String getSiteName(final UUID siteId) {
        return sitesProvider.findByIdOrThrow(siteId).getName();
    }

    /**
     * Since the sites list is static @ runtime (nothing is ever added or deleted from it) we can do this safely.
     */
    public List<AccountType> getSiteWhiteListedAccountType(final UUID siteId) {
        return sitesProvider.findByIdOrThrow(siteId).getAccountTypeWhitelist();
    }

    public List<Site> getSites() {
        return sitesProvider.allSites();
    }


    public static boolean isSiteStubbedByYoltbank(final UUID siteId) {
        return YOLT_BANK_TEST_SITES_IDS.contains(siteId);
    }

}
