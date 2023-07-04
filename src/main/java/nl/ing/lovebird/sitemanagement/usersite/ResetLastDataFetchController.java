package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.managementPortals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "user-sites", description = "internal API to reset the last data fetch.")
public class ResetLastDataFetchController {

    private final UserSiteMaintenanceService userSiteMaintenanceService;

    @Internal(managementPortals)
    @Operation(summary = "Resets the last fetch date for all user-sites for a site. The next time they will be refreshed, a longer period will be fetched.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully reset the last fetch date"
            )
    })
    @PatchMapping(value = "/sites/{siteId}/reset-last-data-fetch", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetLastFetchDate(@PathVariable final UUID siteId,
                                                   @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {

        try (LogBaggage b = LogBaggage.builder().siteId(siteId).build()) {
            log.info("Request to reset last data fetch for all user-sites for a site");
            userSiteMaintenanceService.resetLastDataFetchForSite(new ClientId(clientToken.getClientIdClaim()), siteId)
                    .thenAccept(ids -> log.info("Successfully reset {} user-sites", ids.size()));
            return ResponseEntity.ok().build();
        }
    }
}
