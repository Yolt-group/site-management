package nl.ing.lovebird.sitemanagement.health.activities;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import nl.ing.lovebird.springdoc.annotations.ExternalApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequiredArgsConstructor
@RestController
@Tag(name = "activities")
public class ActivityController {

    private final PersistedActivityService persistedActivityService;

    @Operation(summary = "Retrieve a list of activities that belong to a specific User.",
            description = "Retrieve a list of activities for the User identified by the path parameter userId. " +
                    "The query parameter runningOnly can be used to narrow the results down to activities that are still in progress.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved a list of activities for the User."),
                    @ApiResponse(responseCode = "403", description = "The userId does not match with the id of the logged in User.", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            })
    @External
    @ExternalApi
    @GetMapping(value = "/v1/users/{userId}/activities", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ActivitiesDTO> getActivitiesForUser(
            @Parameter(description = "Unique identifier of the User for which to list activities.", required = true)
            @PathVariable("userId") UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "Optional query parameter to filter for activities that are currently in progress.")
            @RequestParam(required = false, defaultValue = "false", value = "runningOnly") boolean runningOnly
    ) {
        if (!clientUserToken.getUserIdClaim().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var activities = persistedActivityService.getActivitiesForUser(userId)
                .stream()
                .map(ActivityMapper::mapToActivityDTO)
                .filter(it -> !runningOnly || it.getEndTime() != null) // If the event is not running anymore, the endtime should be present
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ActivitiesDTO(activities));
    }
}
