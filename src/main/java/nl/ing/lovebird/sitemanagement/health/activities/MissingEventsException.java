package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;

import java.util.UUID;

class MissingEventsException extends RuntimeException {
    MissingEventsException(final UUID activityId) {
        super(String.format("No events found for activity %s", activityId));
    }

    MissingEventsException(final UUID activityId, EventType eventType) {
        super(String.format("No events of type %s found for activity %s", eventType, activityId));
    }
}
