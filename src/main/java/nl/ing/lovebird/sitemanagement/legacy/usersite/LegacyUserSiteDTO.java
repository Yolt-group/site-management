package nl.ing.lovebird.sitemanagement.legacy.usersite;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;
import nl.ing.lovebird.sitemanagement.site.SiteDTO;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Data
@Deprecated
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "UserSite", description = "The connection between the user and a site.")
public class LegacyUserSiteDTO {

    @Schema(required = true)
    private UUID id;
    @Schema(required = true)
    private UUID siteId;
    @Schema(required = true,
            description = "Some statuses can have an optional reason. This only counts for the error states: STEP_FAILED, " +
                    "LOGIN_FAILED, REFRESH_FAILED, EXTERNAL_PARTY_TECHNICAL_ERROR")
    private LegacyUserSiteStatusCode status;
    private LegacyUserSiteStatusReason reason;
    @Schema(allowableValues = "60",
            description = "The time in seconds the current status is valid. Currently only used for STEP_NEEDED")
    private Long statusTimeoutSeconds;
    @Schema(
            description = "Some state/reason combinations can mean the user needs to take action. This field shows which " +
                    "action the user should take. Note: this can be a retry after fixing something at their bank " +
                    "(for example in case of a locked account).")
    private UserSiteNeededAction action;
    @Schema(required = false,
            description = "This object will be fetched when you add the request param fetchObject=site if it is supported " +
                    "by the endpoint")
    private SiteDTO site;

    @Schema(description = "The current status of this user-site migrating its provider", required = true)
    private MigrationStatus migrationStatus;

    @Schema(description = "The last time user-site was successfully refreshed", required = true)
    private Date lastDataFetch;

    @Schema(description = "The date/time at which the consent for the site will expire.")
    @Nullable
    private Instant externalConsentExpiresAt;

    @Schema(description = "If true, the site to which this user-site is related is no longer supported.")
    private boolean noLongerSupported;

    @Schema(description = "Links to related resources - HATEOAS format", required = true)
    @JsonProperty("_links")
    private UserSiteLinksDTO links;

    @Data
    @AllArgsConstructor
    @Schema(name = "UserSiteLinks", description = "Links related to this UserSite (HATEOAS)")
    public static class UserSiteLinksDTO {

        private LinkDTO site;
        @Schema(required = true)
        private LinkDTO refresh;
        @Schema(required = true)
        private LinkDTO delete;

        @Schema(required = true)
        private LinkDTO migrationGroup;
        /**
         * @deprecated use {@link #renewAccess}. It refers to the same endpoint, but renewConsent was a misleading name.
         * This link should be used for both scraper connections and direct connections.
         */
        @Deprecated
        @Schema(required = true)
        private LinkDTO renewConsent;
        @Schema(required = true)
        private LinkDTO renewAccess;
        @Schema
        private LinkDTO getNextStep;

        public UserSiteLinksDTO(final String sitePath, final String refreshPath, final String deletePath, final String migrationGroupPath, final String renewConsentPath,
                                final String getNextStepPath) {
            site = new LinkDTO(sitePath);
            refresh = new LinkDTO(refreshPath);
            delete = new LinkDTO(deletePath);
            migrationGroup = new LinkDTO(migrationGroupPath);
            renewConsent = new LinkDTO(renewConsentPath);
            renewAccess = new LinkDTO(renewConsentPath);
            getNextStep = new LinkDTO(getNextStepPath);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "Link", description = "Link that can be used to perform an operation related to this object (HATEOAS)")
    public static class LinkDTO {
        private String href;
    }
}
