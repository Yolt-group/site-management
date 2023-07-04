package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.batchTrigger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserSiteCleanupController {
    private final UserSiteCleanupService userSiteCleanupService;

    @Internal(batchTrigger)
    @Operation(summary = "Clean-up UserSites that are waiting more than 'ttl' seconds to complete (mark them as timed-out).")
    @PostMapping(value = "/v1/usersites/loginstep-cleanup", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cleanupUserSitesWaitingInLoginStep(@RequestParam(value = "ttl", defaultValue = "3000") long ttl) {
        userSiteCleanupService.markWaitingInLoginStepAsTimedOut(ttl);
        return accepted().build();
    }
}
