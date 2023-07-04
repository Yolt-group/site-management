package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.events.ActivityEventKey;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.serializer.ActivityEventSerializer;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.dspipeline.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivitiesConsumerTest {

    @Mock
    private ClientUserToken clientUserToken;

    @Mock
    private ActivityService activityService;

    @Mock
    private HealthMetrics healthMetrics;

    @InjectMocks
    private ActivitiesConsumer consumer;

    @Test
    void consumerShouldIncreaseMetricAndPassOverReceivedMessage() {
        var key = new ActivityEventKey(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        var event = new RefreshedUserSiteEvent(key.getUserId(), key.getActivityId(), ZonedDateTime.parse("2019-07-11T14:52:34.501+02:00"),
                UUID.randomUUID(), null, null, null);

        var userContext = UserContext.builder().userId(key.getUserId()).build();

        consumer.activityUpdate(ActivityEventSerializer.serialize(key), userContext.toJson(), clientUserToken, event);

        verify(healthMetrics).incrementReceivedActivityEventOfType(event.getType());
        verify(activityService).handleEvent(clientUserToken, event);
    }
}
