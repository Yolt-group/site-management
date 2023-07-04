package nl.ing.lovebird.sitemanagement.health;

import nl.ing.lovebird.activityevents.events.UpdateUserSiteEvent;
import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import nl.ing.lovebird.sitemanagement.health.ActivityEvent;
import nl.ing.lovebird.sitemanagement.health.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestContainerDataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void canSaveAPostgresActivityEvent() {
        var activityId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var updateUserSiteEvent = new UpdateUserSiteEvent(userId, UUID.randomUUID(), activityId, "site-name", ZonedDateTime.now(Clock.fixed(Instant.ofEpochMilli(1618925809L), ZoneId.of("UTC"))), UUID.randomUUID());
        var event = new ActivityEvent(eventId, activityId, UUID.randomUUID(), updateUserSiteEvent, Instant.ofEpochMilli(1618925809L));

        eventRepository.save(event);

        var persistedEvent = eventRepository.findById(event.getEventId());

        assertThat(persistedEvent)
                .isPresent()
                .get()
                .isEqualTo(event);
    }

}
