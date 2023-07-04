package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.Activity;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersistedActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private HealthMetrics metrics;
    @Mock
    private ActivityEventService activityEventService;

    @InjectMocks
    private PersistedActivityService persistedActivityService;
    public static final UUID ACTIVITY_ID = randomUUID();
    public static final UUID USER_ID = randomUUID();
    public static final UUID USER_SITE_ID = randomUUID();
    public static final ZonedDateTime END_TIME = ZonedDateTime.now();
    private final Activity persistedActivity = new Activity(ACTIVITY_ID, USER_ID, END_TIME.toInstant().minusSeconds(1), null, EventType.REFRESH_USER_SITES, new UUID[]{USER_SITE_ID});

    @BeforeEach
    void setUp() {
        lenient().when(activityRepository.findById(ACTIVITY_ID)).thenReturn(Optional.of(persistedActivity));
    }

    @ParameterizedTest
    @MethodSource("setEndTimeForActivityTestParameters")
    void setEndTimeForSuccessfulActivity_whenPassingAbstractActivity_willOnlyUpdateForTerminatingEvents(AbstractEvent event, int expectedNumberOfCalls) {
        persistedActivityService.updateSuccessfullyFinishedActivity(event);
        verify(activityRepository, times(expectedNumberOfCalls)).save(any());
    }

    @Test
    void setEndTimeForSuccessFulActivity_whenUserSiteIdsDifferent_willUpdateListInDatabase() {
        var argumentCaptor = ArgumentCaptor.forClass(Activity.class);

        var userSiteInfo = TransactionsEnrichmentFinishedEvent.UserSiteInfo.builder()
                .accountId(randomUUID())
                .userSiteId(USER_SITE_ID)
                .oldestChangedTransaction(LocalDate.now())
                .build();
        var persistedActivity = new Activity(ACTIVITY_ID, USER_ID, Instant.now().minusSeconds(1), null, EventType.REFRESH_USER_SITES, new UUID[]{randomUUID()});
        var event = new TransactionsEnrichmentFinishedEvent(USER_ID, ACTIVITY_ID, Collections.singletonList(userSiteInfo), END_TIME, TransactionsEnrichmentFinishedEvent.Status.SUCCESS);

        when(activityRepository.findById(event.getActivityId())).thenReturn(Optional.of(persistedActivity));

        persistedActivityService.updateSuccessfullyFinishedActivity(event);

        verify(activityRepository).save(argumentCaptor.capture());
        verify(metrics).incrementAdditionalUserSitesUpdatedWithEnrichment(EventType.TRANSACTIONS_ENRICHMENT_FINISHED);

        assertThat(argumentCaptor.getValue())
                .isEqualToIgnoringGivenFields(persistedActivity, "userSiteIds", "endTime")
                .returns(new UUID[]{USER_SITE_ID}, Activity::getUserSiteIds)
                .returns(END_TIME.toInstant().truncatedTo(ChronoUnit.MILLIS), Activity::getEndTime);
    }

    @Test
    void setEndTimeForSuccessfulActivity_whenUserSiteIdsUnchanged_willNotUpdateListInDatabase() {
        var argumentCaptor = ArgumentCaptor.forClass(Activity.class);

        var userSiteInfo = TransactionsEnrichmentFinishedEvent.UserSiteInfo.builder()
                .accountId(randomUUID())
                .userSiteId(USER_SITE_ID)
                .oldestChangedTransaction(LocalDate.now())
                .build();
        var event = new TransactionsEnrichmentFinishedEvent(USER_ID, ACTIVITY_ID, Collections.singletonList(userSiteInfo), END_TIME, TransactionsEnrichmentFinishedEvent.Status.SUCCESS);

        persistedActivityService.updateSuccessfullyFinishedActivity(event);
        verify(activityRepository).save(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue())
                .isEqualToIgnoringGivenFields(persistedActivity, "endTime")
                .returns(new UUID[]{USER_SITE_ID}, Activity::getUserSiteIds)
                .returns(END_TIME.toInstant().truncatedTo(ChronoUnit.MILLIS), Activity::getEndTime);
    }

    @ParameterizedTest
    @EnumSource(ConnectionStatus.class)
    void setEndTimeForFailedActivity_whenPassingUserSitesRefreshedEvent_willOnlyPersistForDisconnectedState(ConnectionStatus connectionStatus) {
        var expectedNumberOfCalls = connectionStatus == ConnectionStatus.DISCONNECTED ? 1 : 0;
        var refreshedUserSiteEvent = new RefreshedUserSiteEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), randomUUID(), connectionStatus, null, null);

        persistedActivityService.setEndTimeIfActivityFailed(refreshedUserSiteEvent);

        verify(activityRepository, times(expectedNumberOfCalls)).save(any());
    }

    private static Stream<Arguments> setEndTimeForActivityTestParameters() {
        return Stream.of(
                Arguments.of(new CreateUserSiteEvent(USER_ID, ACTIVITY_ID, randomUUID(), "site-name", ZonedDateTime.now(), randomUUID()), 0),
                Arguments.of(new RefreshedUserSiteEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), randomUUID(), ConnectionStatus.CONNECTED, null, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS), 0),
                Arguments.of(new DeleteUserSiteEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), randomUUID()), 0),
                Arguments.of(new RefreshUserSitesEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), List.of(randomUUID())), 0),
                Arguments.of(new RefreshUserSitesFlywheelEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), List.of(randomUUID())), 0),
                Arguments.of(new UpdateUserSiteEvent(USER_ID, ACTIVITY_ID, randomUUID(), "site-name", ZonedDateTime.now(), randomUUID()), 0),
                Arguments.of(new IngestionFinishedEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), randomUUID(), "", "", Collections.emptyMap(), Collections.emptyMap()), 0),
                Arguments.of(new AggregationFinishedEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now(), EventType.REFRESH_USER_SITES, List.of(randomUUID())), 1),
                Arguments.of(new CounterpartiesFeedbackEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now()), 0),
                Arguments.of(new CategorizationFeedbackEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now()), 0),
                Arguments.of(new TransactionCyclesFeedbackEvent(USER_ID, ACTIVITY_ID, ZonedDateTime.now()), 0)
        );
    }
}
