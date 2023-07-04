package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.sitemanagement.health.ActivityEvent;
import nl.ing.lovebird.sitemanagement.health.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * This service should handle all database communication for activity events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEventService {

    private final EventRepository eventRepository;

    void storeEvent(final @NonNull AbstractEvent event) {
        var eventId = UUID.randomUUID();
        var newEvent = new ActivityEvent(eventId, event.getActivityId(), event.getUserId(), event, getTruncatedInstant(event.getTime()));
        eventRepository.save(newEvent);
    }

    List<AbstractEvent> getAllEvents(final @NonNull UUID activityId) {
        return eventRepository.findAllByActivityIdOrderByEventTimeAsc(activityId).stream()
                .map(ActivityEvent::getEvent)
                .collect(Collectors.toList());
    }

    private Instant getTruncatedInstant(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().truncatedTo(MILLIS);
    }

    int deleteActivitiesOlderThanDays(int days) {
        return eventRepository.deleteActivitiesOlderThanDays(days);
    }
}
