package nl.ing.lovebird.sitemanagement.flows.lib;

import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.sites.RegisteredSite;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.List.of;
import static nl.ing.lovebird.providerdomain.AccountType.*;
import static nl.ing.lovebird.providerdomain.ServiceType.AIS;
import static nl.ing.lovebird.providerdomain.ServiceType.PIS;
import static nl.ing.lovebird.sitemanagement.lib.CountryCode.*;
import static nl.ing.lovebird.sitemanagement.sites.ConsentBehavior.CONSENT_PER_ACCOUNT;

public class TestProviderSites {
    private TestProviderSites() {
    }

    public static final UUID PIS_ENABLED_SITE_ID = UUID.randomUUID();

    public static final RegisteredSite MONZO_SITE = new RegisteredSite(
            "Monzo",
            "MONZO",
            "MONZO",
            UUID.randomUUID(),
            List.of(CURRENT_ACCOUNT, SAVINGS_ACCOUNT),
            List.of(NL, GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite YOLT_TEST_BANK = new RegisteredSite("Yolt test bank",
            "YOLT_PROVIDER",
            null,
            PIS_ENABLED_SITE_ID,
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT, PREPAID_ACCOUNT, PENSION, INVESTMENT),
            of(GB, FR, IT, ES, BE, DE, NL, PL, CZ),
            null,
            null,
            null,
            Map.of(AIS, List.of(LoginRequirement.REDIRECT), PIS, List.of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite ABN_AMRO = new RegisteredSite("ABN AMRO",
            "ABN_AMRO",
            "ABN AMRO",
            UUID.fromString("7670247e-323e-4275-82f6-87f31119dbd3"), //TODO c4po-7504 potentially change to random uuid. Now must be real one since popular countries are still in SM db
            of(CURRENT_ACCOUNT),
            of(NL),
            null,
            Set.of(CONSENT_PER_ACCOUNT),
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite AIB = new RegisteredSite("AIB",
            "AIB",
            "Allied Irish Bank (GB)",
            UUID.fromString("5806ae85-7ee6-48a5-98ea-0f464b9b71cb"), //TODO c4po-7504 potentially change to random uuid. Now must be real one since popular countries are still in SM db
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite LLOYDS = new RegisteredSite("Lloyds",
            "LLOYDS_BANK",
            "Lloyds",
            UUID.fromString("36130c5f-9024-4a89-91fc-be31fac2f9ec"),
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            null,
            Map.of(AIS, of(LoginRequirement.REDIRECT), PIS, of(LoginRequirement.REDIRECT))
    );

    public static final RegisteredSite BARCLAYS_SCRAPING = new RegisteredSite("Barclays",
            "YODLEE",
            "Barclays",
            UUID.fromString("41de5024-d22e-4d24-b295-fd90a4fed926"),
            of(CURRENT_ACCOUNT, SAVINGS_ACCOUNT),
            of(GB),
            null,
            null,
            "4118",
            Map.of(AIS, of(LoginRequirement.FORM))
    );

    public static final UUID RABOBANK_SITE_ID = UUID.fromString("eedd41a8-51f8-4426-9348-314a18dbdec7");
    public static final RegisteredSite RABOBANK = new RegisteredSite("Rabobank",
            "RABOBANK",
            null,
            RABOBANK_SITE_ID,
            of(CURRENT_ACCOUNT, CREDIT_CARD, SAVINGS_ACCOUNT, PREPAID_ACCOUNT, PENSION, INVESTMENT),
            of(GB, FR, IT, ES, BE, DE, NL, PL, CZ),
            null,
            null,
            null,
            Map.of(PIS, List.of(LoginRequirement.REDIRECT))
    );

}