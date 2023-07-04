package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.events.AggregationFinishedEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.dspipeline.RefreshPeriod;
import nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationProducer;
import nl.ing.lovebird.sitemanagement.health.webhook.ClientWebhookService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityEventTestHelper.*;
import static nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEventOrigin.fromStartEventType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregationFinishedServiceTest {

    @Mock
    private HealthMetrics healthMetrics;
    @Mock
    private ActivityEventService activityEventService;
    @Mock
    private ClientWebhookService clientWebhookService;
    @Mock
    private PersistedActivityService persistedActivityService;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private HealthOrchestrationProducer healthOrchestrationProducer;
    @Mock
    private ClientUserToken clientUserToken;

    private AggregationFinishedService aggregationFinishedService;

    @BeforeEach
    void setUp() {
        aggregationFinishedService = new AggregationFinishedService(Clock.systemUTC(),
                healthMetrics, activityEventService, clientWebhookService, persistedActivityService, pipelineService,
                userSiteService, healthOrchestrationProducer);
    }

    @Test
    public void trigger_happyFlow_storesEventConnectsUserSitesTriggersPipelineAndPublishesMetrics() {
        var userId = randomUUID();
        var activityId = randomUUID();
        var userSiteIdOne = UUID.randomUUID();
        var userSiteIdTwo = UUID.randomUUID();
        when(clientUserToken.getUserIdClaim()).thenReturn(userId);

        var startEvent = createStartEvent(activityId, userSiteIdOne, userSiteIdTwo);

        var expectedEvent = AggregationFinishedEvent.builder()
                .activityId(activityId)
                .userId(clientUserToken.getUserIdClaim())
                .initialActivityEventType(startEvent.getType())
                .userSiteIds(List.of(userSiteIdOne))
                .time(ZonedDateTime.now(Clock.systemUTC()))
                .build();

        aggregationFinishedService.trigger(clientUserToken, activityId, List.of(
                startEvent,
                createIngestionFinishedEvent(activityId, userSiteIdOne, new RefreshPeriod(null, null)),
                createRefreshedEvent(activityId, userSiteIdTwo)
        ));

        var actualEventCaptor = ArgumentCaptor.forClass(AggregationFinishedEvent.class);
        verify(activityEventService).storeEvent(actualEventCaptor.capture());
        assertThat(actualEventCaptor.getValue()).usingRecursiveComparison().ignoringFields("time").isEqualTo(expectedEvent);
        assertThat(actualEventCaptor.getValue().getTime()).isCloseTo(expectedEvent.getTime(), within(10, SECONDS));

        verify(userSiteService).markUserSitesConnected(clientUserToken.getUserIdClaim(), List.of(userSiteIdOne));
        verify(pipelineService).startPipeline(clientUserToken, activityId);
        verify(healthOrchestrationProducer).publishRefreshFinishedEvent(clientUserToken, fromStartEventType(startEvent.getType()), activityId, List.of(userSiteIdOne));
        verify(healthMetrics).recordActivityDuration(Duration.between(startEvent.getTime(), actualEventCaptor.getValue().getTime()), startEvent.getType());
    }

    @Test
    public void trigger_customerWithEnrichmentContract_shouldNotFinishEvent() {

        var activityId = randomUUID();
        var userSiteId = UUID.randomUUID();
        var startEvent = createStartEvent(activityId, userSiteId);
        var ingestionFinishedEvent = createIngestionFinishedEvent(activityId, userSiteId, new RefreshPeriod(null, null));

        lenient().when(clientUserToken.hasDataEnrichmentCategorization()).thenReturn(true);
        lenient().when(clientUserToken.hasDataEnrichmentCycleDetection()).thenReturn(true);
        lenient().when(clientUserToken.hasDataEnrichmentLabels()).thenReturn(true);
        lenient().when(clientUserToken.hasDataEnrichmentMerchantRecognition()).thenReturn(true);

        aggregationFinishedService.trigger(clientUserToken, activityId, List.of(startEvent, ingestionFinishedEvent));

        verify(clientWebhookService, never()).push(any(), any(), any());
        verify(persistedActivityService, never()).updateSuccessfullyFinishedActivity(any());
    }

    @Test
    public void trigger_customerWithoutEnrichmentContract_shouldFinishEvent() {

        var activityId = randomUUID();
        var userSiteId = UUID.randomUUID();
        var startEvent = createStartEvent(activityId, userSiteId);
        var ingestionFinishedEvent = createIngestionFinishedEvent(activityId, userSiteId, new RefreshPeriod(null, null));

        aggregationFinishedService.trigger(clientUserToken, activityId, List.of(startEvent, ingestionFinishedEvent));

        var storedEventCaptor = ArgumentCaptor.forClass(AggregationFinishedEvent.class);
        verify(activityEventService).storeEvent(storedEventCaptor.capture());

        verify(clientWebhookService).push(clientUserToken, List.of(startEvent, ingestionFinishedEvent), storedEventCaptor.getValue());
        verify(persistedActivityService).updateSuccessfullyFinishedActivity(storedEventCaptor.getValue());
    }
}
