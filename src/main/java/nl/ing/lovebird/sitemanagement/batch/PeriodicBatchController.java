package nl.ing.lovebird.sitemanagement.batch;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.batchTrigger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This controller is used for all batch jobs that should run on a predetermined interval (e.g. daily or hourly)
 * The configuration of the jobs can be found in the config-server files for the batch-trigger service.
 * Some of these jobs are also configured in the application.yml file in the batch-trigger service.
 *
 * Please put one-time batch jobs in a different controller.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PeriodicBatchController {

    private final ClientTokenRequesterService clientTokenRequesterService;
    private final DiagnosticLoggingService diagnosticLoggingService;
    private final DisconnectUnusableUserSitesService disconnectUnusableUserSitesService;
    private final BatchUserSiteDeleteService batchUserSiteDeleteService;
    private final ConsentTestingService consentTestingService;


    @Internal(batchTrigger)
    @Operation(summary = "Remove usersites of a list of siteIds")
    @PostMapping(value = "/batch/delete-user-sites-for-site", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteUserSitesForSiteId(@RequestParam(required = true) final List<UUID> siteId,
                                                         @RequestParam(defaultValue = "1000") int maxUserSitesPerSiteToDelete,
                                                         @RequestParam(defaultValue = "true") boolean dryrun) {
        for (UUID id : siteId) {
            batchUserSiteDeleteService.scheduleUserSitesForDeletionForSite(id, maxUserSitesPerSiteToDelete, dryrun);
        }
        return ResponseEntity.accepted().build();
    }

    @Internal(batchTrigger)
    @Operation(summary = "Invoking providers consent tests")
    @PostMapping(value = "/batch/{clientId}/{clientRedirectUrlId}/invoke-consent-tests")
    public ResponseEntity<Void> invokeConsentTests(
            @PathVariable final ClientId clientId,
            @PathVariable final UUID clientRedirectUrlId,
            @RequestParam(required = false, defaultValue = "AIS") final ServiceType serviceType
            ) throws HttpException {
        ClientToken clientToken = clientTokenRequesterService.getClientToken(clientId.unwrap());
        consentTestingService.invokeConsentTests(clientRedirectUrlId, clientToken, serviceType);
        return ResponseEntity.ok().build();
    }

    @Internal(batchTrigger)
    @Operation(summary = "Log the number of unique user refreshes per client for the last n days")
    @GetMapping(value = "/batch/unique-refreshes", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logUniqueRefreshes(
            @RequestParam(name = "days-in-past", defaultValue = "7") @Min(value = 1) int daysInPast
    ) {
        diagnosticLoggingService.logNumberOfUniqueUserRefreshes(daysInPast);
        return ResponseEntity.accepted().build();
    }

    @Internal(batchTrigger)
    @Operation(summary = "Log statistics on how often a UserSite status + reason occur in our database.")
    @GetMapping(value = "/batch/log-user-site-status-statistics", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logUserSiteStatusesAndReasonsInUse() {
        diagnosticLoggingService.logUserSiteStatusesAndReasonsInUse();
        return ResponseEntity.accepted().build();
    }

    @Internal(batchTrigger)
    @Operation(summary = "Mark unusable user-sites as disconnected")
    @PostMapping(value = "/batch/user-sites/disconnect-unusable")
    public ResponseEntity<Void> disconnectUnusableUserSites(@RequestParam(defaultValue = "true") boolean dryrun) {
        disconnectUnusableUserSitesService.disconnectAllUnusable(dryrun)
                .thenAccept(count -> log.info("Successfully disconnected {} user-sites via batch", count));
        return ResponseEntity.accepted().build();
    }

}
