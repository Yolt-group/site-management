package nl.ing.lovebird.sitemanagement.sites;

import java.util.Collections;
import java.util.Objects;

public class SitesMapper {

    public Site mapToSite(RegisteredSite registeredSite) {
        return Site.builder()
                .name(registeredSite.getName())
                .provider(registeredSite.getProviderKey())
                .groupingBy(registeredSite.getGroupingBy())
                .id(registeredSite.getId())
                .accountTypeWhitelist(registeredSite.getAccountTypeWhiteList())
                .availableInCountries(registeredSite.getAvailableCountries())
                .consentExpiryInDays(defaultValue(registeredSite.getConsentExpiryInDays(), 90))
                .consentBehavior(defaultValue(registeredSite.getConsentBehavior(), Collections.emptySet()))
                .externalId(registeredSite.getExternalId())
                .usesStepTypes(registeredSite.getUsesStepTypes())
                .build();
    }

    private <T> T defaultValue(T nullable, T defaultvalue) {
        return Objects.nonNull(nullable) ? nullable : defaultvalue;
    }
}
