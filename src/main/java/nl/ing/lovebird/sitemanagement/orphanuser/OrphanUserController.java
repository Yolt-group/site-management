package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.managementPortals;

@RestController
@Slf4j
public class OrphanUserController {

    private final Integer maxOrphanedUsersInResponse;
    private final OrphanUserService orphanUserService;
    private final OrphanUserAllowedExecutions allowedExecutions;

    @Autowired
    public OrphanUserController(
            @Value("${lovebird.providers.form.maxOrphanedUsersInResponse:100}") Integer maxOrphanedUsersInResponse,
            OrphanUserService orphanUserService,
            OrphanUserAllowedExecutions allowedExecutions
    ) {
        this.maxOrphanedUsersInResponse = maxOrphanedUsersInResponse;
        this.orphanUserService = orphanUserService;
        this.allowedExecutions = allowedExecutions;
    }

    @Internal(managementPortals)
    @PostMapping("/orphan-users-batch/{provider}/prepare")
    public ResponseEntity<String> prepareBatch(@PathVariable final String provider,
                                               @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        UUID batchId = orphanUserService.startPreparingBatch(clientToken, provider);
        log.info("Started preparing batch {} for finding orphaned users at {} provider", batchId, provider);
        return ResponseEntity.ok(batchId.toString());
    }

    @Internal(managementPortals)
    @PostMapping("/orphan-users-batch/{provider}/{orphanUserBatchId}/execute")
    public ResponseEntity<Void> executeOrphanUserBatch(@PathVariable final UUID orphanUserBatchId,
                                                       @PathVariable final String provider,
                                                       @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        if (allowedExecutions.isAllowed(provider, orphanUserBatchId)) {
            orphanUserService.executeOrphanUserBatch(clientToken, provider, orphanUserBatchId);
            return ResponseEntity.ok().build();
        } else {
            log.warn("Trying to execute a batch with id {} for provider {} which is not allowed for execution", orphanUserBatchId, provider);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Internal(managementPortals)
    @GetMapping("/orphan-users-batch/{provider}")
    public ResponseEntity<List<OrphanUserBatchDTO>> listOrphanUserBatches(
            @PathVariable final String provider,
            @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        List<OrphanUserBatchDTO> orphanUserBatchList = orphanUserService.listOrphanUserBatches(clientToken, provider);
        return ResponseEntity.ok(orphanUserBatchList);
    }

    @Internal(managementPortals)
    @GetMapping("/orphan-users-batch/{provider}/{orphanUserBatchId}")
    public ResponseEntity<OrphanUserBatchDTO> getOrphanUserBatch(@PathVariable final UUID orphanUserBatchId,
                                                                 @PathVariable final String provider,
                                                                 @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        OrphanUserBatchDTO orphanUserBatch = orphanUserService.getOrphanUserBatch(clientToken, provider, orphanUserBatchId);
        return ResponseEntity.ok(orphanUserBatch);
    }

    @Internal(managementPortals)
    @GetMapping("/orphan-users-batch/{provider}/{orphanUserBatchId}/orphan-users")
    public ResponseEntity<OrphanUserResponseDTO> listOrphanUsers(@PathVariable final UUID orphanUserBatchId,
                                                                 @PathVariable final String provider,
                                                                 @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        List<OrphanUserDTO> orphanUserList = orphanUserService.listOrphanUsers(clientToken, provider, orphanUserBatchId);
        final int actualListSize = orphanUserList.size();
        if (actualListSize > maxOrphanedUsersInResponse) {
            log.warn("Too many orphaned users for provider {} in batch {} - max allowed: {}, actual size: {}. Will return only max allowed.",
                    provider, orphanUserBatchId, maxOrphanedUsersInResponse, actualListSize);
            orphanUserList = new ArrayList<>(orphanUserList.subList(0, maxOrphanedUsersInResponse));
        }
        return ResponseEntity.ok(new OrphanUserResponseDTO(actualListSize, orphanUserList));
    }

    @Internal(managementPortals)
    @DeleteMapping("/orphan-users-batch/{provider}/{orphanUserBatchId}")
    public ResponseEntity<Void> deleteOrphanUserBatch(@PathVariable final UUID orphanUserBatchId,
                                                      @PathVariable final String provider,
                                                      @VerifiedClientToken(restrictedTo = {"assistance-portal-yts"}) ClientToken clientToken) {
        orphanUserService.deleteBatchData(clientToken, provider, orphanUserBatchId);
        return ResponseEntity.ok().build();
    }
}
