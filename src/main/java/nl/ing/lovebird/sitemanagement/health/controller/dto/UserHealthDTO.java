package nl.ing.lovebird.sitemanagement.health.controller.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;

import java.util.List;

@Data
@Schema(name = "UserHealth", description = "Contains health status information for a given user.")
public class UserHealthDTO {

    @NonNull
    @Schema(required = true)
    private final LovebirdHealthCode health;

    @NonNull
    @Schema(required = true)
    @ArraySchema(arraySchema = @Schema(required = true))
    private final List<AccountGroupDTO> accountGroups;

    @NonNull
    @Schema(required = true)
    @ArraySchema(arraySchema = @Schema(required = true))
    private final List<UserSiteHealthDTO> userSites;

    @NonNull
    @Schema(required = true)
    private final MigrationStatus migrationStatus;
}
