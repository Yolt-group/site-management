package nl.ing.lovebird.sitemanagement.legacy.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.site.ConnectionType;
import nl.ing.lovebird.sitemanagement.usersite.FormStepObject;
import nl.ing.lovebird.sitemanagement.usersite.RedirectStepObject;

import java.util.UUID;

@Data
@Deprecated
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LoginFormResponse", description = "Contains the user-site unique id that has been (partially) created. " +
        "To know exactly the status of the Login, call the get user-site status endpoint.")
public class LoginResponseDTO {

    /**
     * If this field is non-null, then For {@link ConnectionType#DIRECT_CONNECTION} it means that the user-site is connected and that we have started to fetch data.
     * For {@link ConnectionType#SCRAPER} on the other hand, logging in and fetching data is the same operation, so even though this field is non-null, it doesn't mean the user-site is connected.
     * This process/activity is started and the activity completes with either LOGIN_FAILED, STEP_NEEDED, or with data.
     */
    @Schema(description = "If not-null, the system has started an activity with this id.")
    private UUID activityId;

    private LegacyStepDTO step;

    private UUID userSiteId;

    private LegacyUserSiteDTO userSite;

    @JsonProperty("_links")
    private LinksDTO links;

    @Data
    @AllArgsConstructor
    @Schema(name = "LoginFormResponseLinks", description = "Links to operations related with this user-site (HATEOAS)")
    public static class LinksDTO {
        @Schema(description = "Where to GET the next form for MFA data",
                allowableValues = "/user-sites/{userSiteId}/mfa")
        private final LinkDTO mfaForm = new LinkDTO("");

        @Schema(description = "Where to GET the status of the user site",
                allowableValues = "/user-sites/{userSiteId}")
        private final LinkDTO userSite;

        @Schema(description = "Where to POST the updated login credentials",
                allowableValues = "/user-sites/{userSiteId}")
        private final LinkDTO updateCredentials = new LinkDTO("");


        public LinksDTO(final String userSitePath) {
            userSite = new LinkDTO(userSitePath);
        }
    }

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "LegacyStep", description = "Contains a step that the user needs to complete in order to successfully connect a user site. If not-null, the user needs to complete this step. This field is non-null if userSite.status == STEP_NEEDED.")
    public static class LegacyStepDTO {
        FormStepObject form;
        RedirectStepObject redirect;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "Link", description = "Link that can be used to perform an operation related to this object (HATEOAS)")
    public static class LinkDTO {
        private String href;
    }
}
