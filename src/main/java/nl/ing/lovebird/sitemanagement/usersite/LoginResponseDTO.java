package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.site.ConnectionType;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LoginFormResponse", description = "Contains the id for the user-site that is being created. " +
        "To know exactly the status of the Login, call the get user-site status endpoint.")
public class LoginResponseDTO {

    /**
     * If this field is non-null, then For {@link ConnectionType#DIRECT_CONNECTION} it means that the user-site is connected and that we have started to fetch data.
     * For {@link ConnectionType#SCRAPER} on the other hand, logging in and fetching data is the same operation, so even though this field is non-null, it doesn't mean the user-site is connected.
     * This process/activity is started and the activity completes with either LOGIN_FAILED, STEP_NEEDED, or with data.
     */
    @Nullable
    @Schema(description = "If not-null, the system has started an activity with this id.")
    private UUID activityId;

    @Nullable
    private StepDTO step;

    @NotNull
    private UUID userSiteId;

    @NotNull
    private UserSiteDTO userSite;

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "Step", description = "Contains a step that the user needs to complete in order to successfully connect a user site. If not-null, the user needs to complete this step. This field is non-null if userSite.connectionStatus == STEP_NEEDED.")
    public static class StepDTO {
        FormStepObject form;
        RedirectStepObject redirect;
    }
}
