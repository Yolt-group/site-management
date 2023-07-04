package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.CreateUserSiteEvent;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.health.Activity;
import nl.ing.lovebird.sitemanagement.health.ActivityEvent;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import nl.ing.lovebird.sitemanagement.health.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class ActivityServiceIntegrationTest {

    @Autowired
    ActivityService activityService;

    @Autowired
    ActivityRepository activityRepository;
    @Autowired
    EventRepository eventRepository;

    @Test
    public void test_deleteActivitiesOlderThanDays() {
        var old = ZonedDateTime.now().minusDays(2)
                .truncatedTo(SECONDS);
        var older = old.minusDays(4).toInstant()
                .truncatedTo(SECONDS);

        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        // Create two activities, we expect the old one to be deleted and the new one to not be touched by the delete.
        var newActivity = new Activity(activityId, userId, old.toInstant(), null, EventType.CREATE_USER_SITE, new UUID[]{});
        var oldActivity = new Activity(activityId, userId, older, null, EventType.CREATE_USER_SITE, new UUID[]{});
        activityRepository.save(newActivity);
        activityRepository.save(oldActivity);
        // Create two events, we expect the old one to be deleted and the new one to not be touched by the delete.
        final CreateUserSiteEvent fake = new CreateUserSiteEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "covfefe", old, UUID.randomUUID());
        var newEvent = new ActivityEvent(UUID.randomUUID(), activityId, userId, fake, old.toInstant());
        var oldEvent = new ActivityEvent(UUID.randomUUID(), activityId, userId, fake, older);
        eventRepository.save(newEvent);
        eventRepository.save(oldEvent);

        // Function under test, will remove everything older than now() - interval '1 days'
        activityService.deleteActivitiesOlderThanDays(3).join();

        // Check that the state is as expected.
        assertThat(activityRepository.getAllByUserIdAndStartEventTypeIn(userId, Set.of(EventType.CREATE_USER_SITE)))
                .containsExactlyInAnyOrder(newActivity);
        assertThat(eventRepository.findAllByActivityIdOrderByEventTimeAsc(activityId))
                .usingElementComparatorIgnoringFields("event")
                .containsExactlyInAnyOrder(newEvent);
    }

}
