package nl.ing.lovebird.sitemanagement.health.orchestration;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Collection;
import java.util.UUID;

@Value
@Builder
@EqualsAndHashCode
public class HealthOrchestrationEvent {

    @NonNull
    HealthOrchestrationEventType type;

    /**
     * An identifier with which a consumer can correlate events related to the same refresh.
     */
    @NonNull
    UUID correlationId;

    /**
     * The ids of UserSites affected by this event.
     */
    @NonNull
    Collection<UUID> userSiteIds;

    /**
     * The initial action that ultimately caused this event to be published.
     */
    @NonNull
    HealthOrchestrationEventOrigin origin;

}
