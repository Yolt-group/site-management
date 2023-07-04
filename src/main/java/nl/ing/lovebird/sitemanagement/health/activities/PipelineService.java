package nl.ing.lovebird.sitemanagement.health.activities;

import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.dspipeline.*;
import nl.ing.lovebird.sitemanagement.health.service.AccountsServiceV1;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class PipelineService {

    private final AccountsServiceV1 accountsService;
    private final ActivityEventService activityEventService;
    private final StartDatasciencePipelineProducer startDatasciencePipelineProducer;

    void startPipeline(final @NonNull ClientUserToken clientUserToken,
                       final @NonNull UUID activityId) {
        startPipeline(clientUserToken, activityId, calculateFinalRefreshPeriod(activityId));
    }

    void startPipelineWithoutRefreshPeriod(final @NonNull ClientUserToken clientUserToken,
                                           final @NonNull UUID activityId) {
        startPipeline(clientUserToken, activityId, null);
    }

    private void startPipeline(final @NonNull ClientUserToken clientUserToken,
                               final @NonNull UUID activityId,
                               final RefreshPeriod refreshPeriod) {
        // trace id propagation is handled by sleuth.
        // This value should be ignored by consumers
        // This value should be consistent because it is part of a kafka key used to guarantee ordering
        var requestTraceId = new UUID(0, 0);

        var accountsContext = accountsService.getAccounts(clientUserToken).stream()
                .map(accountDTO -> new DatasciencePipelinePayload.AccountContext(accountDTO.id, accountDTO.type))
                .toList();

        var datasciencePipelineValue = new DatasciencePipelineValue(
                new Headers(DatasciencePipelinePayload.MESSAGE_TYPE, clientUserToken.getUserIdClaim(), requestTraceId),
                new DatasciencePipelinePayload(
                        activityId,
                        refreshPeriod,
                        // Wit the removal of the app the users preferred currency is always EUR
                        // the country is always NL.
                        new UserContext("EUR", "NL", Collections.emptyList(), clientUserToken.getClientIdClaim(), clientUserToken.getUserIdClaim()),
                        accountsContext
                )
        );
        startDatasciencePipelineProducer.sendRefreshTriggeredEvent(clientUserToken, datasciencePipelineValue);
    }

    @VisibleForTesting
    RefreshPeriod calculateFinalRefreshPeriod(final UUID activityId) {
        List<IngestionFinishedEvent> ingestionFinishedEvents = activityEventService
                .getAllEvents(activityId)
                .stream()
                .filter(event -> event.getType().equals(EventType.INGESTION_FINISHED))
                .map(event -> (IngestionFinishedEvent) event)
                .toList();
        if (ingestionFinishedEvents.isEmpty()) {
            throw new MissingEventsException(activityId, EventType.INGESTION_FINISHED);
        }

        Optional<String> minimumStartYearMonth = ingestionFinishedEvents.stream()
                .map(IngestionFinishedEvent::getStartYearMonth)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());
        Optional<String> maximumEndYearMonth = ingestionFinishedEvents.stream()
                .map(IngestionFinishedEvent::getEndYearMonth)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());

        if (minimumStartYearMonth.isEmpty() || maximumEndYearMonth.isEmpty()) {
            log.info("Cannot calculate final refresh period; no IngestionFinished events found with non-empty " +
                            "StartYearMonth ({}) and EndYearMonth ({})!",
                    minimumStartYearMonth.orElse(null),
                    maximumEndYearMonth.orElse(null));
            return null;
        }

        return new RefreshPeriod(minimumStartYearMonth.get(), maximumEndYearMonth.get());
    }

}
