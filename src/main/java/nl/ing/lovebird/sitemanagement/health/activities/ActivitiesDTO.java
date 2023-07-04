package nl.ing.lovebird.sitemanagement.health.activities;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@Schema(name = "ActivitiesDTO", description = "Contains a list of recent activities for a user.")
public class ActivitiesDTO {
    @NotNull
    @ArraySchema(arraySchema = @Schema(description = "A list of recent activities for a user. Can be an empty list if there were no recent activities", required = true))
    List<ActivityDTO> activities;

    public enum ActivityType {
        CATEGORIZATION_FEEDBACK,
        COUNTERPARTIES_FEEDBACK,
        TRANSACTION_CYCLES_FEEDBACK,
        CREATE_USER_SITE,
        DELETE_USER_SITE,
        REFRESH_USER_SITES,
        REFRESH_USER_SITES_FLYWHEEL,
        UPDATE_USER_SITE
    }
}
