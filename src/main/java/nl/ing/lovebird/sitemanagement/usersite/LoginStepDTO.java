package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.UUID;


@Value
@Schema(name = "LoginStep", description = "The response when initiating the process to connect a user to a site. " +
        "This object contains *either* a Form Step Object, *or* a Redirect Step Object.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginStepDTO {
    @Schema(description = "Deprecated. Please don't use/parse this. Will be removed", required = true)
    @Deprecated
    LinksDTO _links;
    FormStepObject form;
    RedirectStepObject redirect;
    @Schema(description = "When calling the /initiate-user-site endpoint, this property refers to a user site that does not exist yet. " +
            "In that case it's a reserved identifier. " +
            "In case the step isn't completed this identifier will expire and will not be used anywhere.")
    UUID userSiteId;

    public LoginStepDTO(String postPath, String url, UUID userSiteId, String state) {
        this._links = new LinksDTO(postPath);
        this.form = null;
        this.redirect = new RedirectStepObject(url, state);
        this.userSiteId = userSiteId;
    }

    public LoginStepDTO(String postPath, FormStepObject form, UUID userSiteId) {
        this._links = new LinksDTO(postPath);
        this.form = form;
        this.redirect = null;
        this.userSiteId = userSiteId;
    }

    @Data
    @Schema(name = "LoginStepLinks", description = "Links related to the LoginStep object (HATEOAS)")
    public static class LinksDTO {

        @Schema(description = "Where to POST the step to.", required = true,
                allowableValues = "/user-sites/123")
        private final LinkDTO postLoginStep;


        LinksDTO(final String postPath) {
            postLoginStep = new LinkDTO(postPath);
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

