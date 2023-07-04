package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Data
@NoArgsConstructor
@Schema(name = "LoginStep", description = "The response when initiating the process to connect a user to a site. " +
        "This object contains *either* a Form Step Object, *or* a Redirect Step Object.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginStepV1DTO {
    @Nullable
    FormStepObject form;
    @Nullable
    RedirectStepObject redirect;
    @NonNull
    @Schema(description = "When calling the /connect endpoint, this property refers to a user site that does not exist yet. " +
            "In that case it's a reserved identifier. " +
            "In case the step isn't completed this identifier will expire and will not be used anywhere.")
    UUID userSiteId;

    public LoginStepV1DTO(@NonNull String url, @NonNull String state, @NonNull UUID userSiteId) {
        this.form = null;
        this.redirect = new RedirectStepObject(url, state);
        this.userSiteId = userSiteId;
    }

    public LoginStepV1DTO(@NonNull FormStepObject form, @NonNull UUID userSiteId) {
        this.form = form;
        this.redirect = null;
        this.userSiteId = userSiteId;
    }
}

