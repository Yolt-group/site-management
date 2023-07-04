package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.util.UUID;

@Value
public class RefreshResponseDTO {
    @Schema(description = "If non-null then a data fetch has started that involves 1 or more user sites.  If null then no data fetch could be started.")
    UUID activityId;
}
