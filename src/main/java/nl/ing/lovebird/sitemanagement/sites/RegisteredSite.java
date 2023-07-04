package nl.ing.lovebird.sitemanagement.sites;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Value;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisteredSite {

    /**
     * Name of the site, this is shown to an end-user and is thus language dependent.
     */
    String name;
    /**
     * Identifier under which records are stored in DB
     */
    String providerKey;
    /**
     * Used to indicate that several sites form a "group", more about this in /docs/concepts/site.md
     */
    String groupingBy;
    /**
     * Unique identifier of a site.  Do **NOT** ever change this.  Generate a new one using for instance `uuidgen`.
     */
    UUID id;
    /**
     * What account types are supported for this site via the {@link #provider}?
     */
    List<AccountType> accountTypeWhiteList;
    /**
     * In what countries is the site available?  Believe this is only cosmetic, no logic based on this.
     */
    List<CountryCode> availableCountries;
    /**
     * Once a user gives us consent to view their data, for how long does that consent remain valid?
     * <p>
     * Note: values for this are all over the place, don't trust it.
     */
    Integer consentExpiryInDays; // default to 90
    /**
     * Does the bank only permit a user to give consent to a single account per user site?
     */
    Set<ConsentBehavior> consentBehavior; // default empty list
    /**
     * Only relevant for scraping sites.  Identifies the bank with which the scraping party must connect.
     */
    String externalId;
    /**
     * Indicates what type of steps are necessary *per* {@link ServiceType}.
     * Deprecated and will be removed as part of C4PO-8806.
     * We should add and use the List<LoginRequirement> loginRequirements field instead.
     * See YCO-1679 for more details
     */
    @Deprecated
    Map<ServiceType, List<LoginRequirement>> usesStepTypes;
}
