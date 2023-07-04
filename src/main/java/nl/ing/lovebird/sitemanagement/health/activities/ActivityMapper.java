package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.sitemanagement.health.activities.ActivitiesDTO.ActivityType;
import nl.ing.lovebird.sitemanagement.health.Activity;
import org.springframework.lang.NonNull;

import java.util.Arrays;

@Slf4j
public class ActivityMapper {
    private ActivityMapper() {
    }

    static ActivityDTO mapToActivityDTO(Activity activity) {
        return new ActivityDTO(activity.getActivityId(),
                mapStartEventToActivityType(activity.getStartEventType()),
                activity.getStartTime(),
                activity.getEndTime(),
                Arrays.asList(activity.getUserSiteIds()));
    }

    private static ActivityType mapStartEventToActivityType(@NonNull EventType eventType) {
        return switch (eventType) {
            case CREATE_USER_SITE -> ActivityType.CREATE_USER_SITE;
            case UPDATE_USER_SITE -> ActivityType.UPDATE_USER_SITE;
            case DELETE_USER_SITE -> ActivityType.DELETE_USER_SITE;
            case REFRESH_USER_SITES -> ActivityType.REFRESH_USER_SITES;
            case REFRESH_USER_SITES_FLYWHEEL -> ActivityType.REFRESH_USER_SITES_FLYWHEEL;
            case COUNTERPARTIES_FEEDBACK -> ActivityType.COUNTERPARTIES_FEEDBACK;
            case CATEGORIZATION_FEEDBACK -> ActivityType.CATEGORIZATION_FEEDBACK;
            case TRANSACTION_CYCLES_FEEDBACK -> ActivityType.TRANSACTION_CYCLES_FEEDBACK;

            // The following EventTypes are not considered start events so we cannot map them to an ActivityType.
            case REFRESHED_USER_SITE, TRANSACTIONS_ENRICHMENT_FINISHED, UNKNOWN, AGGREGATION_FINISHED, INGESTION_FINISHED -> {
                var message = String.format("Attempt to map unsupported start event type: %s", eventType.name());
                throw new IllegalActivityStateException(message);
            }
        };
    }
}
