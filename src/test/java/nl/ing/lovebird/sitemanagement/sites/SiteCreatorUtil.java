package nl.ing.lovebird.sitemanagement.sites;

import lombok.NonNull;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SiteCreatorUtil {

    /**
     * Method to create a site with required args.
     */
    public static Site createTestSite(String id, String name, String provider, List<AccountType> accountTypeWhitelist, List<CountryCode> availableCountries, Map<ServiceType, List<LoginRequirement>> usesStepTypes) {
        return Site.site(id, name, provider, accountTypeWhitelist, availableCountries)
                .usesStepTypes(usesStepTypes)
                .build();
    }

    /**
     * Method to create a site with required args.
     */
    public static Site createTestSite(@NonNull UUID id, @NonNull String name, @NonNull String provider, @NonNull List<AccountType> accountTypeWhitelist, @NonNull List<CountryCode> availableInCountries, @NonNull Map<ServiceType, List<LoginRequirement>> usesStepTypes, String groupingBy, String externalId, Integer consentExpiryInDays, Set<ConsentBehavior> consentBehavior) {
        return  Site.builder()
                .name(name)
                .provider(provider)
                .id(id)
                .groupingBy(groupingBy)
                .accountTypeWhitelist(accountTypeWhitelist)
                .consentBehavior(consentBehavior)
                .consentExpiryInDays(consentExpiryInDays)
                .externalId(externalId)
                .usesStepTypes(usesStepTypes)
                .availableInCountries(availableInCountries)
                .build();
    }
}
