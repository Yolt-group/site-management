package nl.ing.lovebird.sitemanagement.health.activities;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Schema(name = "ActivityDTO", description = "A single activity for a user that is executed on one or multiple user-sites. If the activity is still in progress, there will be no end time set for the activity.")
public class ActivityDTO {
    @NotNull
    @Schema(description = "The id of the activity")
    UUID activityId;
    @NotNull
    @Schema(description = "The type of the activity")
    ActivitiesDTO.ActivityType activity;
    @NotNull
    @Schema(description = "The timestamp of starting the activity")
    Instant startTime;
    @Nullable
    @Schema(description = "The optional timestamp of finishing the activity. If the activity is still in progress, this will not be set")
    Instant endTime;
    @NotNull
    @ArraySchema(arraySchema = @Schema(description = "The list of usersites that were changed with this activity"))
    List<UUID> userSites;
}
