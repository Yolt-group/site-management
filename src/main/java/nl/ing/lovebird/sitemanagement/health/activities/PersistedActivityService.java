package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics.ActivityLifecycleCheckpoint;
import nl.ing.lovebird.sitemanagement.health.Activity;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;
import static nl.ing.lovebird.activityevents.EventType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistedActivityService {
    private static final Set<EventType> YTS_START_EVENTS = Set.of(
            CREATE_USER_SITE,
            UPDATE_USER_SITE,
            DELETE_USER_SITE,
            REFRESH_USER_SITES,
            REFRESH_USER_SITES_FLYWHEEL,
            COUNTERPARTIES_FEEDBACK,
            CATEGORIZATION_FEEDBACK,
            TRANSACTION_CYCLES_FEEDBACK
    );

    private final HealthMetrics metrics;
    private final ActivityRepository activityRepository;

    /**
     * This is used to create a new activity for the given start event.
     * Within YTS we don't have concrete activities. An activity is represented by the set of Events that happen that
     * are related to each other with the same overlapping {@link AbstractEvent#getActivityId()}.
     * <p>
     * For administrative purposes, we create the "parent activity" in our database.
     */
    void persistNewActivity(AbstractEvent event) {
        if (event instanceof StartEvent) {

            // track start of activity
            metrics.incrementActivityLifecycleCheckpoint(ActivityLifecycleCheckpoint.START, event.getType());

            var userSiteIds = getUUIDsForEvent(event).orElseGet(() -> new UUID[0]);

            var activity = new Activity(event.getActivityId(),
                    event.getUserId(),
                    getTruncatedInstant(event.getTime()),
                    null,
                    event.getType(),
                    userSiteIds);

            activityRepository.save(activity);
        }
    }

    /**
     * Returns a list of {@link Activity} for a user.
     * Because Yolt App is also sending events through our system, we have to filter out all non-YTS start events.
     * If no relevant activity can be found this list is empty.
     */
    List<Activity> getActivitiesForUser(final @NonNull UUID userId) {
        return activityRepository.getAllByUserIdAndStartEventTypeIn(userId, YTS_START_EVENTS);
    }

    /**
     * If the activity fails to ingest all of the usersites it is considered finished as well.
     * This should be determined by the calling method, ideally by using the {@link ActivityDoneChecker#isLastProcessedUserSite(List, AbstractEvent)}.
     * <p>
     * This method is used to register the end time of an activity if it failed. The end time should be determined by
     * looking at the final {@link RefreshedUserSiteEvent} we receive with a the {@link ConnectionStatus#DISCONNECTED}.
     *
     * @throws IllegalActivityStateException if no activity can be found for the activity id.
     */
    void setEndTimeIfActivityFailed(RefreshedUserSiteEvent disconnectedUserSiteEvent) {
        if (disconnectedUserSiteEvent.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {

            var activity = activityRepository.findById(disconnectedUserSiteEvent.getActivityId())
                    .orElseThrow(() -> new IllegalActivityStateException(String.format("Activity with id %s does not exist.", disconnectedUserSiteEvent.getActivityId())));

            // track unsuccessfully finished activity
            metrics.incrementActivityLifecycleCheckpoint(ActivityLifecycleCheckpoint.FAILURE, activity.getStartEventType());

            // mark as finished by setting the end-time of the activity
            activity.setEndTime(getTruncatedInstant(disconnectedUserSiteEvent.getTime()));

            activityRepository.save(activity);
        }
    }

    /**
     * This method is used to set the end time for an activity, the end time is determined by looking at the last event of the activity.
     * An activity can only be finished with an event of type {@link AggregationFinishedEvent} or {@link TransactionsEnrichmentFinishedEvent}.
     * Other subclasses of {@link AbstractEvent} cannot mark the end of an activity.
     * <p>
     * If the set of updated user-site ids differs from the expected set of user-site-ids we will increment a counter.
     * A different set of updated user-sites should only be possible for events of type {@link TransactionsEnrichmentFinishedEvent}.
     *
     * @throws IllegalActivityStateException if no activity can be found for the activity id.
     */
    void updateSuccessfullyFinishedActivity(final @NonNull AbstractEvent finalEvent) {
        if (!(finalEvent instanceof AggregationFinishedEvent) && !(finalEvent instanceof TransactionsEnrichmentFinishedEvent)) {
            return;
        }

        var activity = activityRepository.findById(finalEvent.getActivityId())
                .orElseThrow(() -> new IllegalActivityStateException(String.format("Activity with id %s does not exist.", finalEvent.getActivityId())));

        // track successfully finished activity
        metrics.incrementActivityLifecycleCheckpoint(ActivityLifecycleCheckpoint.SUCCESS, activity.getStartEventType());

        // mark as finished by setting the end-time of the activity
        activity.setEndTime(getTruncatedInstant(finalEvent.getTime()));

        var affectedUserSiteIds = getAffectedUserSiteIdsFromEvent(finalEvent);
        // We only want to update the set of usersite ids affected by this activity if it changed.
        if (!affectedUserSiteIds.equals(new HashSet<>(Arrays.asList(activity.getUserSiteIds())))) {
            activity.setUserSiteIds(affectedUserSiteIds.toArray(UUID[]::new));
            log.info("Found a different set of user sites that were updated for this activity with id {}.", finalEvent.getActivityId());
            metrics.incrementAdditionalUserSitesUpdatedWithEnrichment(finalEvent.getType());
        }

        activityRepository.save(activity);
    }

    private Set<UUID> getAffectedUserSiteIdsFromEvent(AbstractEvent event) {
        return switch (event.getType()) {
            case AGGREGATION_FINISHED -> new HashSet<>(((AggregationFinishedEvent) event).getUserSiteIds());
            case TRANSACTIONS_ENRICHMENT_FINISHED -> ((TransactionsEnrichmentFinishedEvent) event).getUserSiteInfo().stream().map(TransactionsEnrichmentFinishedEvent.UserSiteInfo::getUserSiteId).collect(Collectors.toSet());
            default -> Collections.emptySet();
        };
    }

    private Optional<UUID[]> getUUIDsForEvent(final @NonNull AbstractEvent event) {
        if (event instanceof UserSiteStartEvent) {
            return Optional.of(((UserSiteStartEvent) event).getUserSiteIds().toArray(UUID[]::new));
        }
        return Optional.empty();
    }

    private Instant getTruncatedInstant(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().truncatedTo(MILLIS);
    }
}
