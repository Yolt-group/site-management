package nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.site.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * @deprecated 'with Country' is deprecated all together.
 */
@Deprecated
@Value
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SiteWithCountry", description = "A site can be a bank, a credit card company, etc. This site entity is decorated with deprecated properties country and popular.")
public class SiteWithCountryDTO extends SiteDTO {

    /**
     * @deprecated we don't keep track of this any longer.  Legacy Yolt App feature.
     */
    @Deprecated
    CountryCode country = CountryCode.GB;

    /**
     * @deprecated we don't keep track of this any longer.  Legacy Yolt App feature.
     */
    @Deprecated
    Boolean popular = false;

    public SiteWithCountryDTO(UUID id, String name, String primaryLabel, List<AccountType> supportedAccountTypes, LoginType loginType, ConnectionType connectionType, List<ServiceType> services, SiteLinksDTO links, String groupingBy, Map<ServiceType, List<LoginRequirement>> usesStepTypes, List<CountryCode> availableInCountries, SiteConnectionHealthStatus health, Boolean noLongerSupported) {
        super(id, name, primaryLabel, supportedAccountTypes, loginType, connectionType, services, links, groupingBy, usesStepTypes, availableInCountries, health, noLongerSupported);
    }
}
