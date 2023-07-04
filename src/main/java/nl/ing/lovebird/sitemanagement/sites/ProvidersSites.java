package nl.ing.lovebird.sitemanagement.sites;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor
public class ProvidersSites {

    @Deprecated
    List<RegisteredSite> registeredSites;
    List<RegisteredSite> aisSiteDetails;

}
