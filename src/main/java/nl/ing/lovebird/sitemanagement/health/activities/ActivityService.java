package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import nl.ing.lovebird.sitemanagement.health.webhook.ClientWebhookService;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.Status.SUCCESS;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityDoneChecker.isLastProcessedUserSite;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ActivityService {

    /**
     * Sometimes, we spontaneously get data from a scraper. They have a background refresh that pushes data to us.
     * In this situation, no 'activity' has been created yet, although it's a required field everywhere.
     * <p>
     * We use this sentinel value to signal that there is new data, albeit without a corresponding activity.
     */
    private static final UUID ACTIVITY_ID_OF_SPONTANEOUS_CALLBACK_NOT_TRIGGERED_BY_USER_ACTIVITY = new UUID(0, 0);

    private final Clock clock;

    private final ActivityRepository activityRepository;
    private final ActivityEventService activityEventService;
    private final AggregationFinishedService aggregationFinishedService;
    private final ClientWebhookService clientWebhookService;
    private final PersistedActivityService persistedActivityService;
    private final PipelineService pipelineService;
    private final UserSiteService userSiteService;

    /**
     * This method is used to determine whether we are done fetching data and/or ingesting the data we fetched for an activity.
     * <p>
     * The {@link ActivityDoneChecker} is used to determine if we received all expected events for the UserSites
     * associated with this activity.
     * <p>
     * If we managed to ingest >= 1 UserSite, an activity is considered successful.
     * If we did not manage to ingest any UserSite we will push a webhook and register the endtime if the
     * status of the UserSite is {@link ConnectionStatus#DISCONNECTED}.
     */
    public void handleFailedRefresh(final @NonNull ClientUserToken clientUserToken,
                                    final @NonNull UUID activityId,
                                    final @NonNull UUID userSiteId,
                                    final @NonNull ConnectionStatus connectionStatus,
                                    final FailureReason failureReason,
                                    final @NonNull RefreshedUserSiteEvent.Status status) {
        var event = new RefreshedUserSiteEvent(clientUserToken.getUserIdClaim(), activityId, ZonedDateTime.now(clock), userSiteId,
                map(connectionStatus), map(failureReason), status);

        activityEventService.storeEvent(event);

        var relatedEvents = getRelatedEvents(activityId);

        if (isLastProcessedUserSite(relatedEvents, event)) {
            if (ingestionFinishedForActivity(relatedEvents)) {
                log.info("anyMatch: IngestionFinishedEvent: true");
                aggregationFinishedService.trigger(clientUserToken, activityId, relatedEvents);
            } else {
                clientWebhookService.push(clientUserToken, getActivityEvents(activityId), event);
                persistedActivityService.setEndTimeIfActivityFailed(event);
            }
        }
    }

    public void handleFailedRefresh(final @NonNull ClientUserToken clientUserToken,
                                    final @NonNull UUID activityId,
                                    final @NonNull PostgresUserSite userSite,
                                    final @NonNull RefreshedUserSiteEvent.Status status) {
        handleFailedRefresh(clientUserToken, activityId, userSite.getUserSiteId(), userSite.getConnectionStatus(),
                userSite.getFailureReason(), status);
    }

    void handleEvent(final @NonNull ClientUserToken clientUserToken,
                     final @NonNull AbstractEvent event) {
        log.debug("handleEvent: handling event {}", event.getClass().getName());

        if (event instanceof StartEvent typedEvent) {
            startActivity(clientUserToken, typedEvent);
        } else if (event instanceof IngestionFinishedEvent typedEvent) {
            handleIngestionFinishedEvent(clientUserToken, typedEvent);
        }
        // Terminal events
        else if (event instanceof TransactionsEnrichmentFinishedEvent typedEvent) {
            handleTransactionsEnrichmentFinishedEvent(clientUserToken, typedEvent);
        } else {
            log.warn("Received unexpected event: {}, ignoring it", event.getClass().getName());
        }
    }

    public List<AbstractEvent> getActivityEvents(final UUID activityId) {
        return activityEventService.getAllEvents(activityId);
    }

    public static StartEvent getStartEvent(@NonNull final List<AbstractEvent> allEvents, final UUID activityId) {
        if (allEvents.isEmpty()) {
            throw new MissingEventsException(activityId);
        }

        return allEvents.stream()
                .filter(StartEvent.class::isInstance)
                .map(StartEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity with id " + activityId + " does not have a start event"));
    }

    public void startActivity(final @NonNull ClientUserToken clientUserToken,
                              final @NonNull StartEvent event) {
        activityEventService.storeEvent(event);
        persistedActivityService.persistNewActivity(event);

        if (event instanceof DeleteUserSiteEvent ||
                event instanceof CategorizationFeedbackEvent ||
                event instanceof CounterpartiesFeedbackEvent ||
                event instanceof TransactionCyclesFeedbackEvent) {
            pipelineService.startPipelineWithoutRefreshPeriod(clientUserToken, event.getActivityId());
        }
    }

    private void handleIngestionFinishedEvent(final @NonNull ClientUserToken clientUserToken,
                                              final @NonNull IngestionFinishedEvent event) {
        updateLastDataFetch(clientUserToken.getUserIdClaim(), event.getUserSiteId(), event.getTime());

        if (isSpontaneousCallback(event)) {
            handleSpontaneousCallback(clientUserToken, event);
            return;
        }

        activityEventService.storeEvent(event);

        var activityId = event.getActivityId();
        var relatedEvents = getRelatedEvents(activityId);

        if (isLastProcessedUserSite(relatedEvents, event)) {
            aggregationFinishedService.trigger(clientUserToken, activityId, relatedEvents);
        }

        clientWebhookService.push(clientUserToken, getActivityEvents(activityId), event);
    }

    private void handleTransactionsEnrichmentFinishedEvent(
            final @NonNull ClientUserToken clientUserToken,
            final @NonNull TransactionsEnrichmentFinishedEvent event) {
        activityEventService.storeEvent(event);

        var relatedEvents = getActivityEvents(event.getActivityId());

        if (event.getStatus() != SUCCESS) {
            log.info("Enrichment for activity {} was not successful ({}).", event.getActivityId(), event.getStatus());
            clientWebhookService.push(clientUserToken, relatedEvents, event);

            return;
        }

        // This check will be investigated as part of story YTRN-1159.
        if (aggregationFinishedForActivity(relatedEvents)) {
            log.info("Enrichment for activity {} finished.", event.getActivityId());

            clientWebhookService.push(clientUserToken, relatedEvents, event);
            persistedActivityService.updateSuccessfullyFinishedActivity(event);
        }
    }

    /**
     * Delete activities older than X days.
     */
    @Async
    @Transactional
    public CompletableFuture<Integer> deleteActivitiesOlderThanDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be > 0");
        }

        long msStart = System.currentTimeMillis();
        int deletedActivityEvents = activityRepository.deleteActivityEventsOlderThanDays(days);
        long msDeletedActivityEvents = System.currentTimeMillis();
        int deletedActivities = activityEventService.deleteActivitiesOlderThanDays(days);
        long msEnd = System.currentTimeMillis();
        if (deletedActivityEvents + deletedActivities > 0) {
            log.info("deleteActivitiesOlderThanDays: activity_events={} ({}ms) activity={} ({}ms)", deletedActivityEvents, msDeletedActivityEvents - msStart, deletedActivities, msEnd - msDeletedActivityEvents);
        }

        return CompletableFuture.completedFuture(deletedActivities);
    }

    private List<AbstractEvent> getRelatedEvents(final @NonNull UUID activityId) {
        return activityEventService.getAllEvents(activityId);
    }

    private void updateLastDataFetch(@NonNull UUID userId, @NonNull UUID userSiteId, @NonNull ZonedDateTime lastDataFetch) {
        try {
            final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
            userSiteService.updateLastDataFetch(userSite, lastDataFetch.toInstant());
        } catch (UserSiteNotFoundException e) {
            log.warn("Could not find user site " + userSiteId + ". It was probably already deleted.", e);
        }
    }

    private boolean ingestionFinishedForActivity(List<AbstractEvent> abstractEvents) {
        return abstractEvents.stream().anyMatch(it -> it instanceof IngestionFinishedEvent);
    }

    private boolean aggregationFinishedForActivity(List<AbstractEvent> abstractEvents) {
        return abstractEvents.stream().anyMatch(it -> it instanceof AggregationFinishedEvent);
    }

    private boolean isSpontaneousCallback(IngestionFinishedEvent event) {
        return ACTIVITY_ID_OF_SPONTANEOUS_CALLBACK_NOT_TRIGGERED_BY_USER_ACTIVITY.equals(event.getActivityId());
    }


    private void handleSpontaneousCallback(final @NonNull ClientUserToken clientUserToken,
                                           final @NonNull IngestionFinishedEvent event) {
        // We are getting data without an initial event. This is because a scraper spontaneously pushes it.
        // We are now creating an ad-hoc start-event:
        UUID adHocActivityId = UUID.randomUUID();


        var startEvent = new RefreshUserSitesFlywheelEvent(
                event.getUserId(),
                adHocActivityId,
                ZonedDateTime.now(clock),
                Collections.singletonList(event.getUserSiteId())
        );
        activityEventService.storeEvent(startEvent);
        persistedActivityService.persistNewActivity(startEvent);

        activityEventService.storeEvent(new IngestionFinishedEvent(
                event.getUserId(),
                adHocActivityId,
                ZonedDateTime.now(clock),
                event.getUserSiteId(),
                event.getStartYearMonth(),
                event.getEndYearMonth(),
                event.getAccountIdToAccountInformation(),
                event.getAccountIdToOldestTransactionChangeDate()
        ));

        pipelineService.startPipeline(clientUserToken, adHocActivityId);
    }

    private nl.ing.lovebird.activityevents.events.FailureReason map(nl.ing.lovebird.sitemanagement.usersite.FailureReason failureReason) {
        if (failureReason == null) {
            return null;
        }

        return switch (failureReason) {
            case TECHNICAL_ERROR -> nl.ing.lovebird.activityevents.events.FailureReason.TECHNICAL_ERROR;
            case ACTION_NEEDED_AT_SITE -> nl.ing.lovebird.activityevents.events.FailureReason.ACTION_NEEDED_AT_SITE;
            case AUTHENTICATION_FAILED -> nl.ing.lovebird.activityevents.events.FailureReason.AUTHENTICATION_FAILED;
            case CONSENT_EXPIRED -> nl.ing.lovebird.activityevents.events.FailureReason.CONSENT_EXPIRED;
        };
    }

    private nl.ing.lovebird.activityevents.events.ConnectionStatus map(nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus connectionStatus) {
        return switch (connectionStatus) {
            case CONNECTED -> nl.ing.lovebird.activityevents.events.ConnectionStatus.CONNECTED;
            case DISCONNECTED -> nl.ing.lovebird.activityevents.events.ConnectionStatus.DISCONNECTED;
            case STEP_NEEDED -> nl.ing.lovebird.activityevents.events.ConnectionStatus.STEP_NEEDED;
        };
    }
}
