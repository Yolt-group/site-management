package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.UserSiteInfo;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import nl.ing.lovebird.sitemanagement.health.dspipeline.RefreshPeriod;
import nl.ing.lovebird.sitemanagement.health.webhook.ClientWebhookService;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityEventTestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

    private static final UUID ACTIVITY_ID = randomUUID();
    private static final UUID USER_SITE_ID = randomUUID();

    @Mock
    private ActivityEventService activityEventService;
    @Mock
    private PersistedActivityService persistedActivityService;
    @Mock
    private AggregationFinishedService aggregationFinishedService;
    @Mock
    private ClientWebhookService clientWebhookService;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private UserSiteService userSiteService;

    @Mock
    private ClientUserToken clientUserToken;

    private ActivityService activityService;

    @BeforeEach
    public void setup() {
        activityService = new ActivityService(
                Clock.systemUTC(),
                activityRepository,
                activityEventService,
                aggregationFinishedService,
                clientWebhookService,
                persistedActivityService,
                pipelineService,
                userSiteService);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(pipelineService);
    }

    @Test
    public void testIngestionUpdates() {
        UUID userId = UUID.randomUUID();
        UUID activityId = randomUUID();

        RefreshUserSitesEvent refreshUserSitesEvent = new RefreshUserSitesEvent(userId, activityId, ZonedDateTime.now(), Collections.singletonList(USER_SITE_ID));
        IngestionFinishedEvent ingestionFinishedEvent = new IngestionFinishedEvent(userId, activityId, ZonedDateTime.now(), USER_SITE_ID, "2000-01", "2000-02", Collections.emptyMap(), Collections.emptyMap());

        PostgresUserSite userSite = mock(PostgresUserSite.class);

        mockEventsSaved(activityId, refreshUserSitesEvent, ingestionFinishedEvent);
        when(userSiteService.getUserSite(userId, USER_SITE_ID)).thenReturn(userSite);

        when(clientUserToken.getUserIdClaim()).thenReturn(userId);
        activityService.handleEvent(clientUserToken, ingestionFinishedEvent);

        verify(userSiteService).updateLastDataFetch(userSite, ingestionFinishedEvent.getTime().toInstant());
        verify(aggregationFinishedService).trigger(clientUserToken, activityId, List.of(refreshUserSitesEvent, ingestionFinishedEvent));
    }

    @Test
    public void incomingRefreshedUserSiteEventAndNoIngestionFinishedEvents_doesNotTriggerPipeline() {
        UUID userSiteId = randomUUID();

        RefreshUserSitesEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId);

        mockEventsSaved(ACTIVITY_ID, startEvent);

        activityService.handleFailedRefresh(clientUserToken, ACTIVITY_ID, userSiteId, ConnectionStatus.DISCONNECTED,
                FailureReason.ACTION_NEEDED_AT_SITE, RefreshedUserSiteEvent.Status.NEW_STEP_NEEDED);

        verifyNoInteractions(pipelineService);
    }

    @Test
    public void when_IGetAnIngestionFinishedEventWithUUID00_then_theDSPipelineShouldBeTriggered() {
        List<AbstractEvent> events = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            AbstractEvent activityEvent = invocation.getArgument(0);
            events.add(activityEvent);
            return null;
        }).when(activityEventService).storeEvent(any(AbstractEvent.class));

        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID specialEmptyActivityId = new UUID(0, 0);
        when(clientUserToken.getUserIdClaim()).thenReturn(userId);
        IngestionFinishedEvent ingestionFinishedEvent = createIngestionFinishedEvent(specialEmptyActivityId, userSiteId, new RefreshPeriod(null, null));
        activityService.handleEvent(clientUserToken, ingestionFinishedEvent);

        verify(pipelineService).startPipeline(eq(clientUserToken), any(UUID.class));

        ArgumentCaptor<AbstractEvent> savedEvents = ArgumentCaptor.forClass(AbstractEvent.class);
        verify(activityEventService, times(2)).storeEvent(savedEvents.capture());
        assertThat(savedEvents.getAllValues().get(0).getActivityId()).isNotEqualTo(specialEmptyActivityId);
        assertThat(savedEvents.getAllValues().get(0).getActivityId()).isEqualTo(savedEvents.getAllValues().get(1).getActivityId());
    }

    @Test
    public void testDeleteUserSiteEvent() {
        UUID userId = clientUserToken.getUserIdClaim();

        DeleteUserSiteEvent deleteUserSiteEvent = new DeleteUserSiteEvent(userId, ACTIVITY_ID, ZonedDateTime.now(), USER_SITE_ID);

        activityService.handleEvent(clientUserToken, deleteUserSiteEvent);

        verify(pipelineService).startPipelineWithoutRefreshPeriod(clientUserToken, ACTIVITY_ID);
    }

    @Test
    public void testShouldTriggerPipelineForThisLastEvent_noStartEvent() {
        final UUID userId = randomUUID();
        IngestionFinishedEvent ingestionFinishedEvent = new IngestionFinishedEvent(userId,
                ACTIVITY_ID,
                ZonedDateTime.now(),
                randomUUID(),
                "",
                "",
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        when(clientUserToken.getUserIdClaim()).thenReturn(userId);
        assertThrows(IllegalActivityStateException.class, () -> activityService.handleEvent(clientUserToken, ingestionFinishedEvent));
    }

    @Test
    public void testShouldNotTriggerDSPipelineIfThereAreNoAccounts() {
        UUID userSiteId = randomUUID();
        UUID userSiteId2 = randomUUID();
        UUID userSiteIdLastEventNoAccounts = randomUUID();

        RefreshUserSitesEvent startEventJson = createStartEvent(ACTIVITY_ID, userSiteId, userSiteId2, userSiteIdLastEventNoAccounts);
        RefreshedUserSiteEvent refreshedEventJson = createRefreshedEvent(ACTIVITY_ID, userSiteId);
        RefreshedUserSiteEvent refreshedEventJson2 = createRefreshedEvent(ACTIVITY_ID, userSiteId2);
        RefreshedUserSiteEvent refreshedEventUserSiteIdLastEventNoAccounts = createRefreshedEvent(ACTIVITY_ID, userSiteIdLastEventNoAccounts);

        mockEventsSaved(ACTIVITY_ID, startEventJson, refreshedEventJson, refreshedEventJson2, refreshedEventUserSiteIdLastEventNoAccounts);

        activityService.handleFailedRefresh(clientUserToken, ACTIVITY_ID, userSiteIdLastEventNoAccounts, ConnectionStatus.CONNECTED,
                null, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS);

        verifyNoInteractions(pipelineService);
    }


    @Test
    public void testActivityEventsAreObtained() {
        CreateUserSiteEvent createUserSiteEvent = new CreateUserSiteEvent(randomUUID(), randomUUID(), randomUUID(), "someSite", ZonedDateTime.now(), randomUUID());
        List<AbstractEvent> expectedEvents = Collections.singletonList(createUserSiteEvent);

        when(activityEventService.getAllEvents(createUserSiteEvent.getActivityId())).thenReturn(expectedEvents);

        List<AbstractEvent> actualEvents = activityService.getActivityEvents(createUserSiteEvent.getActivityId());

        assertThat(actualEvents).isEqualTo(expectedEvents);
    }

    private void mockEventsSaved(UUID activityId, AbstractEvent... events) {
        when(activityEventService.getAllEvents(activityId)).thenReturn(List.of(events));
    }

    @Test
    public void getFirstEvent_whenEmpty_shouldThrowException() {
        assertThrows(MissingEventsException.class, () -> ActivityService.getStartEvent(Collections.emptyList(), randomUUID()));
    }

    @Test
    public void getFirstEvent_whenNotEmpty_shouldReturnFirst() {
        var expectedEvent = createStartEvent(ACTIVITY_ID, randomUUID());
        var refreshedEvent = createRefreshedEvent(ACTIVITY_ID, randomUUID());

        var actualEvent = ActivityService.getStartEvent(List.of(refreshedEvent, expectedEvent), randomUUID());

        assertThat(actualEvent).isEqualTo(expectedEvent);
    }

    @Test
    public void handleRefreshedUserSiteEvent_whenAlreadyReceivedIngestionFinishedEventAndThisIsLastReceivedEvent_shouldTriggerAggregationFinished() {
        UUID userSiteId = randomUUID();

        RefreshUserSitesEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId);
        IngestionFinishedEvent ingestionFinishedEvent = createIngestionFinishedEvent(ACTIVITY_ID, userSiteId, new RefreshPeriod(null, null));

        // Note that we FIRST "saved" the ingestion finished event
        mockEventsSaved(ACTIVITY_ID, startEvent, ingestionFinishedEvent);

        activityService.handleFailedRefresh(clientUserToken, ACTIVITY_ID, userSiteId, ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, RefreshedUserSiteEvent.Status.FAILED);

        verify(aggregationFinishedService).trigger(clientUserToken, ACTIVITY_ID, List.of(startEvent, ingestionFinishedEvent));
    }

    @Test
    public void handleTransactionEnrichmentFinishedEvent_happyFlow_shouldFinishEvent() {
        UUID userSiteId = UUID.randomUUID();
        List<UserSiteInfo> userSiteInfos = List.of(UserSiteInfo.builder()
                .userSiteId(userSiteId)
                .accountId(userSiteId)
                .oldestChangedTransaction(LocalDate.EPOCH)
                .build());

        RefreshUserSitesEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId);
        IngestionFinishedEvent ingestionFinishedEvent = createIngestionFinishedEvent(ACTIVITY_ID, userSiteId, new RefreshPeriod(null, null));
        AggregationFinishedEvent aggregationFinishedEvent = createAggregationFinishedEvent(ACTIVITY_ID, EventType.REFRESH_USER_SITES_FLYWHEEL, List.of(userSiteId));
        TransactionsEnrichmentFinishedEvent transactionsEnrichmentFinishedEvent = createTransactionsEnrichmentFinishedEvent(
                ACTIVITY_ID, userSiteInfos, TransactionsEnrichmentFinishedEvent.Status.SUCCESS);

        mockEventsSaved(ACTIVITY_ID, startEvent, ingestionFinishedEvent, aggregationFinishedEvent);

        activityService.handleEvent(clientUserToken, transactionsEnrichmentFinishedEvent);

        verify(clientWebhookService).push(clientUserToken, List.of(startEvent, ingestionFinishedEvent, aggregationFinishedEvent),
                transactionsEnrichmentFinishedEvent);
        verify(persistedActivityService).updateSuccessfullyFinishedActivity(transactionsEnrichmentFinishedEvent);
    }
}
