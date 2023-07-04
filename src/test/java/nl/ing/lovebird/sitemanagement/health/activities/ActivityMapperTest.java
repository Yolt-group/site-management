package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.sitemanagement.health.activities.ActivitiesDTO.ActivityType;
import nl.ing.lovebird.sitemanagement.health.Activity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityMapperTest {

    @Test
    void mapToActivityDTO_whenCorrectActivityIsPassed_willMap() {
        var userId = UUID.randomUUID();
        var activityId = UUID.randomUUID();
        var activityStartTime = ZonedDateTime.now();
        var userSiteIds = List.of(UUID.randomUUID());

        var activity = new Activity(activityId, userId, activityStartTime.toInstant(), Instant.now(), EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});

        var activityDTO = ActivityMapper.mapToActivityDTO(activity);

        assertThat(activityDTO)
                .returns(activity.getActivityId(), ActivityDTO::getActivityId)
                .returns(ActivityType.REFRESH_USER_SITES, ActivityDTO::getActivity)
                .returns(activity.getStartTime(), ActivityDTO::getStartTime)
                .returns(activity.getEndTime(), ActivityDTO::getEndTime)
                .returns(Arrays.asList(activity.getUserSiteIds()), ActivityDTO::getUserSites);
    }
}
