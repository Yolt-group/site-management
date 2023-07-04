package nl.ing.lovebird.sitemanagement.sites;

import lombok.*;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.types.SiteId;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;

import java.util.*;

/**
 * The entity to which this services owes its name: a site
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class Site {

    static Site.SiteBuilder site(@NonNull String id, @NonNull String name, @NonNull String provider, @NonNull List<AccountType> accountTypeWhitelist, @NonNull List<CountryCode> availableInCountries) {
        return Site.builder()
                .id(UUID.fromString(id))
                .name(name)
                .provider(provider)
                .accountTypeWhitelist(accountTypeWhitelist)
                .availableInCountries(availableInCountries);
    }

    /**
     * Name of the site, this is shown to an end-user and is thus language dependent.
     */
    @NonNull String name;

    /**
     * The provider module with which we connect to the site.
     */
    @NonNull String provider;

    /**
     * Used to indicate that several sites form a "group", more about this in /docs/concepts/site.md
     */
    String groupingBy;

    /**
     * Unique identifier of a site.  Do **NOT** ever change this.  Generate a new one using for instance `uuidgen`.
     */
    @NonNull UUID id;

    /**
     * What account types are supported for this site via the {@link #provider}?
     */
    @NonNull List<AccountType> accountTypeWhitelist;

    /**
     * In what countries is the site available?  Believe this is only cosmetic, no logic based on this.
     */
    @NonNull List<CountryCode> availableInCountries;

    /**
     * Once a user gives us consent to view their data, for how long does that consent remain valid?
     * <p>
     * Note: values for this are all over the place, don't trust it.
     */
    @Builder.Default
    Integer consentExpiryInDays = 90;

    /**
     * Does the bank only permit a user to give consent to a single account per user site?
     */
    @Builder.Default
    Set<ConsentBehavior> consentBehavior = Collections.emptySet();

    /**
     * Only relevant for scraping sites.  Identifies the bank with which the scraping party must connect.
     */
    String externalId;

    /**
     * Indicates what type of steps are necessary *per* {@link ServiceType}.
     */
    @NonNull Map<ServiceType, List<LoginRequirement>> usesStepTypes;

    /**
     * Available services can be derived from {@link #usesStepTypes}
     */
    public List<ServiceType> getServices() {
        return new ArrayList<>(usesStepTypes.keySet());
    }


    /**
     * coalesce(grouping_by, name).  This preserves previous behaviour.
     */
    public String getGroupingBy() {
        return groupingBy != null ? groupingBy : name;
    }

    public SiteId getWrappedId() {
        return new SiteId(id);
    }
}
