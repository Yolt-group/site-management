package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.events.UpdateUserSiteEvent;
import nl.ing.lovebird.sitemanagement.health.ActivityEvent;
import nl.ing.lovebird.sitemanagement.health.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityEventServiceTest {

    private static final ZonedDateTime EVENT_TIMESTAMP_ZONED_DATETIME = ZonedDateTime.now(Clock.fixed(Instant.ofEpochMilli(1618925809L), ZoneId.of("UTC")));
    private static final Instant EVENT_TIMESTAMP_INSTANT = Instant.ofEpochMilli(1618925809L);

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ActivityEventService activityEventService;

    @Test
    void storeEvent_whenInvoked_willStoreInPostgres() {
        var userId = randomUUID();
        var activityId = randomUUID();
        var event = new UpdateUserSiteEvent(userId, randomUUID(), activityId, "sitename", EVENT_TIMESTAMP_ZONED_DATETIME, randomUUID());

        var postgresActivityEventCaptor = ArgumentCaptor.forClass(ActivityEvent.class);

        // When
        activityEventService.storeEvent(event);

        // Verify that the repository is invoked.
        verify(eventRepository).save(postgresActivityEventCaptor.capture());

        assertThat(postgresActivityEventCaptor.getValue())
                .returns(activityId, ActivityEvent::getActivityId)
                .returns(userId, ActivityEvent::getUserId)
                .returns(EVENT_TIMESTAMP_INSTANT, ActivityEvent::getEventTime)
                .returns(event, ActivityEvent::getEvent)
                .extracting(ActivityEvent::getEventId)
                .isNotNull();
    }

    @Test
    void getAllEvents_whenEventsAreInPostgres_willReturnThem() {
        var eventId = randomUUID();
        var activityId = randomUUID();
        var userId = randomUUID();
        var updateUserSiteEvent = new UpdateUserSiteEvent(userId, UUID.randomUUID(), activityId, "site-name", EVENT_TIMESTAMP_ZONED_DATETIME, UUID.randomUUID());
        when(eventRepository.findAllByActivityIdOrderByEventTimeAsc(activityId)).thenReturn(Collections.singletonList(new ActivityEvent(eventId, activityId, userId, updateUserSiteEvent, EVENT_TIMESTAMP_INSTANT)));

        var postgresEvents = activityEventService.getAllEvents(activityId);

        assertThat(postgresEvents.get(0)).isEqualTo(updateUserSiteEvent);

    }

}
