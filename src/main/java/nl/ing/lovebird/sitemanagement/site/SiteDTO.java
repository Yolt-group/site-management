package nl.ing.lovebird.sitemanagement.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SiteEntity", description = "A site can be a bank, a credit card company, etc.")
public class SiteDTO {

    @Schema(required = true, allowableValues = "123")
    private UUID id;
    @Schema(required = true, allowableValues = "Bank X open banking")
    private String name;

    /**
     * @deprecated
     * remove this field, since on version 1.30 the app started using SiteDTO#supportedAccountTypes.
     */
    @Deprecated
    @Schema(required = true, description = "The label to show", example = "Current accounts")
    private String primaryLabel;
    @ArraySchema(arraySchema = @Schema(required = true, description = "The account types this site supports", example = "CURRENT_ACCOUNT,SAVINGS_ACCOUNT,CREDIT_CARD"))
    private List<AccountType> supportedAccountTypes;
    /**
     * @deprecated To see which 'login steps' are required. See {@link #usesStepTypes}.
     * To see whether it's scraper or not, see {@link #connectionType}
     */
    @Deprecated
    @Schema(required = true, allowableValues = "FORM,URL",
            description = "The type of login that can be expected.")
    private LoginType loginType;
    @Schema(required = true, allowableValues = "SCRAPER,DIRECT_CONNECTION",
            description = "Indicates whether this is a direct connection or through a scraper.")
    private ConnectionType connectionType;

    @ArraySchema(arraySchema = @Schema(required = true, description = "The service types this site supports", example = "AIS,PIS"))
    private List<ServiceType> services;
    @Schema(description = "Links to related resources - HATEOAS format", required = true)
    @JsonProperty("_links")
    private SiteLinksDTO links;
    @Schema(required = false, allowableValues = "Bank X")
    private String groupingBy;
    @ArraySchema(arraySchema = @Schema(required = true, description = "The step types used for each of the supported services", example = "AIS: [REDIRECT]"))
    private Map<ServiceType, List<LoginRequirement>> usesStepTypes;
    @ArraySchema(arraySchema = @Schema(required = true, description = "The list of countries the site is available in", example = "'GB','NL'"))
    private List<CountryCode> availableInCountries;
    @Schema(required = true, description = "An object containing information about the health of the site")
    private SiteConnectionHealthStatus health;
    @Schema(required = true, description = "If set to true, this site is no longer supported.")
    private Boolean noLongerSupported;

    @Data
    @Schema(name = "SiteLinks", description = "Links related to this site (HATEOAS)")
    public static class SiteLinksDTO {

        @Schema(required = true, allowableValues = "/sites/96da1236-1bf7-45e0-b87d-1fd8e264255b")
        private final LinkDTO self;
        @Schema(required = true, allowableValues = "/content/images/sites/logos/96da1236-1bf7-45e0-b87d-1fd8e264255b.png")
        private final LinkDTO logo;
        @Schema(required = true, allowableValues = "/content/images/sites/icons/96da1236-1bf7-45e0-b87d-1fd8e264255b.png")
        private final LinkDTO icon;

        @Schema(
                required = true,
                allowableValues = "/sites/96da1236-1bf7-45e0-b87d-1fd8e264255b/initiate-user-site, /sites/v2/96da1236-1bf7-45e0-b87d-1fd8e264255b/migration-login/81bc1236-1bf7-45e0-b87d-2ff8e364255b",
                description = "the login can contain a step to connect a usersite, for either normal or migration purpose. The server will determine which one.")
        private final LinkDTO initiateUserSite;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "Link", description = "Link that can be used to perform an operation related to this object (HATEOAS)")
    public static class LinkDTO {
        private String href;
    }
}
