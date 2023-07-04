package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.activityevents.events.AggregationFinishedEvent;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationProducer;
import nl.ing.lovebird.sitemanagement.health.webhook.ClientWebhookService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityService.getStartEvent;
import static nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEventOrigin.fromStartEventType;

/**
 * The AggregationFinishedService is used to notify the Yolt services that data fetching has been completed and that we are
 * starting the data enrichment part of the refresh flow.
 * <p>
 * A full refresh goes as follows:
 * <p>
 * - A refresh is started for one or more user-sites, this creates an activity
 * - After data fetching is done by the providers pod, we ingest the data in A&T
 * - A&T will emit an ingestion event for each fully ingested user-site
 * - The health service will emit an aggregation event to notify the system that ingestion and aggregation of all refreshed user-sites is finished
 * - DS will start processing the data and emit multiple enrichment events depending on the enabled services for the particular client we refreshed the user-sites for.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationFinishedService {

    private final Clock clock;
    private final HealthMetrics healthMetrics;

    private final ActivityEventService activityEventService;
    private final ClientWebhookService clientWebhookService;
    private final PersistedActivityService persistedActivityService;
    private final PipelineService pipelineService;
    private final UserSiteService userSiteService;
    private final HealthOrchestrationProducer healthOrchestrationProducer;

    void trigger(@NonNull ClientUserToken clientUserToken, UUID activityId, List<AbstractEvent> relatedEvents) {
        var startEvent = getStartEvent(relatedEvents, activityId);
        var userSiteIds = getIngestedUserSites(relatedEvents);
        var event = AggregationFinishedEvent.builder()
                .activityId(startEvent.getActivityId())
                .userId(clientUserToken.getUserIdClaim())
                .time(ZonedDateTime.now(clock))
                .userSiteIds(userSiteIds)
                .initialActivityEventType(startEvent.getType())
                .build();

        activityEventService.storeEvent(event);

        userSiteService.markUserSitesConnected(clientUserToken.getUserIdClaim(), userSiteIds);

        // Trigger enrichment pipeline even when the user doesn't have an enrichment contract, see this message:
        // https://lovebirdteam.slack.com/archives/CNF2YANLR/p1649950334190739?thread_ts=1649943998.313389&cid=CNF2YANLR
        pipelineService.startPipeline(clientUserToken, event.getActivityId());

        healthOrchestrationProducer.publishRefreshFinishedEvent(clientUserToken, fromStartEventType(startEvent.getType()),
                activityId, userSiteIds);
        healthMetrics.recordActivityDuration(Duration.between(startEvent.getTime(), event.getTime()), startEvent.getType());

        // If the user doesn't have an enrichment contract, the activity is finished. Mark it as such.
        if (!hasEnrichmentEnabled(clientUserToken)) {
            clientWebhookService.push(clientUserToken, relatedEvents, event);
            persistedActivityService.updateSuccessfullyFinishedActivity(event);
        }
    }

    private boolean hasEnrichmentEnabled(ClientUserToken clientUserToken) {
        return clientUserToken.hasDataEnrichmentCategorization() ||
                clientUserToken.hasDataEnrichmentMerchantRecognition() ||
                clientUserToken.hasDataEnrichmentCycleDetection() ||
                clientUserToken.hasDataEnrichmentLabels();
    }

    private List<UUID> getIngestedUserSites(final List<AbstractEvent> allEvents) {
        return allEvents.stream()
                .filter(abstractEvent -> abstractEvent.getType() == EventType.INGESTION_FINISHED)
                .map(IngestionFinishedEvent.class::cast)
                .map(IngestionFinishedEvent::getUserSiteId)
                .collect(toList());
    }
}
