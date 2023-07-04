package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.batchTrigger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
public class InternalActivityController {

    private final ActivityService activityService;

    @Internal(batchTrigger)
    @PostMapping(value = "/internal/cleanup-old-activities", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ActivitiesDTO> getActivitiesForUser(
            @RequestParam(defaultValue = "7") int days
    ) {
        activityService.deleteActivitiesOlderThanDays(days);
        return ResponseEntity.accepted().build();
    }
}
