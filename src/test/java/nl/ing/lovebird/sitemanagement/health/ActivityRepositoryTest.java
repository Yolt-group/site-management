package nl.ing.lovebird.sitemanagement.health;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.sitemanagement.configuration.TestContainerDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestContainerDataJpaTest
class ActivityRepositoryTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @Test
    void save_canPersistAnActivity() {
        var newActivity = new Activity(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), null, EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});
        activityRepository.save(newActivity);

        var persistedActivity = activityRepository.findById(newActivity.getActivityId());
        assertThat(persistedActivity)
                .isPresent()
                .get()
                .isEqualTo(newActivity);
    }

    @Transactional
    @Test
    void save_willOnlyUpdateTheEndTimeAndListOfUserSites() {
        var updatedUuids = new UUID[]{UUID.randomUUID()};
        var newActivity = new Activity(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), null, EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});
        var persistedActivity = activityRepository.save(newActivity);

        // Update the end time and user-site ids.
        var endTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        persistedActivity.setEndTime(endTime);
        persistedActivity.setUserSiteIds(updatedUuids);

        activityRepository.save(persistedActivity);
        // We need to flush and clear the entitymanager to see the actual changes in the database
        entityManager.flush();
        entityManager.clear();

        var updatedActivity = activityRepository.findById(newActivity.getActivityId());
        assertThat(updatedActivity)
                .isPresent()
                .get()
                .returns(endTime, Activity::getEndTime)
                .returns(updatedUuids, Activity::getUserSiteIds);
    }

    @Transactional
    @Test
    void getAllByUserIdAndStartEventTypeIn_givenASetOfEvents_willFilterOnEventType() {
        var newActivity = new Activity(UUID.randomUUID(), USER_ID, Instant.now(), null, EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});
        var targetActivity = activityRepository.save(newActivity);
        var otherActivity = new Activity(UUID.randomUUID(), USER_ID, Instant.now(), null, EventType.CREATE_USER_SITE, new UUID[]{UUID.randomUUID()});
        activityRepository.save(otherActivity);

        var filteredUserSites = activityRepository.getAllByUserIdAndStartEventTypeIn(USER_ID, Set.of(EventType.REFRESH_USER_SITES));

        assertThat(filteredUserSites)
                .returns(1, List::size)
                .extracting(it -> it.get(0))
                .isEqualTo(targetActivity);
    }

    @Test
    void getAllByUserIdAndStartEventTypeIn_givenASetOfNotFoundEventTypes_willReturnEmptyList() {
        var userId = UUID.randomUUID();
        var newActivity = new Activity(UUID.randomUUID(), userId, Instant.now(), null, EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});
        activityRepository.save(newActivity);

        var filteredUserSites = activityRepository.getAllByUserIdAndStartEventTypeIn(userId, Set.of(EventType.INGESTION_FINISHED));

        assertThat(filteredUserSites).returns(0, List::size);
    }
}
