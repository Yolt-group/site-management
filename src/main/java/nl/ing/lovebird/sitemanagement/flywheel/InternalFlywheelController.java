package nl.ing.lovebird.sitemanagement.flywheel;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalTime;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.batchTrigger;
import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.managementPortals;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalFlywheelController {

    private final Clock clock;
    private final InternalFlywheelService internalFlywheelService;

    @Internal(batchTrigger)
    @Operation(description = "Internal flywheel to refresh user sites (not accessible from security proxy)")
    @PostMapping("/flywheel/internal")
    public ResponseEntity<Void> refreshAllUserSites() {
        log.debug("Controller started internal flywheel");
        internalFlywheelService.refreshUserSitesAsync(LocalTime.now(clock));
        return ResponseEntity.accepted().build();
    }

    @Internal(managementPortals)
    @Operation(description = "Internal flywheel to refresh user sites for specific user (not accessible from security proxy)")
    @PostMapping("/flywheel/internal/users/{userId}")
    public ResponseEntity<Void> refreshAllUserSitesForUser(@PathVariable UUID userId) {
        log.debug("Controller started internal flywheel for {}", userId);
        internalFlywheelService.forceRefreshUserSitesForSpecificUserAsync(userId);
        return ResponseEntity.accepted().build();
    }
}
