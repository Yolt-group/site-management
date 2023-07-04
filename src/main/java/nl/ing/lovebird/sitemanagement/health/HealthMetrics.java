package nl.ing.lovebird.sitemanagement.health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.AggregationFinishedEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class HealthMetrics {
    private final MeterRegistry meterRegistry;

    /**
     * Time the total duration of an activity.  The total duration is computed as the delta between the time recorded
     * between the {@link AggregationFinishedEvent} and its corresponding startEvent.
     */
    public void recordActivityDuration(@NonNull Duration duration, @NonNull EventType initialActivityEvent) {
        meterRegistry.timer("site_activity_total_duration", "initial_activity_type", initialActivityEvent.name())
                .record(duration);
    }

    /**
     * Monitor the amount of received activity events per {@link EventType}.
     */
    public void incrementReceivedActivityEventOfType(@NonNull EventType type) {
        meterRegistry.counter("activity_events_received", "type", type.name())
                .increment();
    }

    /**
     * Monitor the amount of sent activity events of a given {@link EventType}.
     */
    public void incrementSentActivityEventOfType(EventType type) {
        meterRegistry.counter("activity_events_sent", "type", type.name())
                .increment();
    }

    /**
     * Monitor the amount of received activity events per type.
     */
    public void incrementReceivedUserEventOfType(String userUpdatedMessageType) {
        meterRegistry.counter("user_events_received", "type", userUpdatedMessageType)
                .increment();
    }

    public void incrementSentDSPipelineTriggerEvents() {
        meterRegistry.counter("pipeline_triggers_events_sent")
                .increment();
    }

    public void incrementAdditionalUserSitesUpdatedWithEnrichment(EventType finalEventType) {
        meterRegistry.counter("transaction_enrichment_updated_additional_user_sites",
                "final_event_type", finalEventType.name())
                .increment();
    }

    public void incrementActivityLifecycleCheckpoint(@NonNull final ActivityLifecycleCheckpoint checkpoint, @NonNull final EventType startEventType) {
        Counter.builder("activity_lifecycle_checkpoints")
                .tag("checkpoint", checkpoint.name())
                .tag("start_event", startEventType.toString())
                .register(meterRegistry)
                .increment();
    }

    public enum ActivityLifecycleCheckpoint {
        /**
         * Indicates the start of an activity
         */
        START,
        /**
         * Indicates that the activity was completed successfully
         */
        SUCCESS,
        /**
         * Indicates that the activity failed to successfully complete
         */
        FAILURE
    }
}

