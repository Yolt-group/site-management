package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.UserSiteInfo;
import nl.ing.lovebird.sitemanagement.health.dspipeline.RefreshPeriod;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ActivityEventTestHelper {

    private final static UUID USER_ID = UUID.randomUUID();

    public static RefreshUserSitesEvent createStartEvent(final UUID activityId, final UUID... userSiteIds) {
        return new RefreshUserSitesEvent(USER_ID, activityId, ZonedDateTime.now(), Arrays.asList(userSiteIds));
    }

    public static RefreshedUserSiteEvent createRefreshedEvent(final UUID activityId, final UUID userSiteId) {
        return new RefreshedUserSiteEvent(USER_ID, activityId,
                ZonedDateTime.now(), userSiteId, null, null, RefreshedUserSiteEvent.Status.OK_SUSPICIOUS);
    }

    public static IngestionFinishedEvent createIngestionFinishedEvent(final UUID activityId, final UUID userSiteId, final RefreshPeriod refreshPeriod) {
        return createIngestionFinishedEvent(activityId, userSiteId, Clock.systemUTC(), refreshPeriod.getStartYearMonth(),
                refreshPeriod.getEndYearMonth());
    }

    public static AggregationFinishedEvent createAggregationFinishedEvent(final UUID activityId, final EventType startEventType, final List<UUID> userSiteIds) {
        return new AggregationFinishedEvent(USER_ID, activityId, ZonedDateTime.now(), startEventType, userSiteIds);
    }

    private static IngestionFinishedEvent createIngestionFinishedEvent(final UUID activityId, final UUID userSiteId, final Clock clock, final String startYearMonth, final String endYearMonth) {
        return IngestionFinishedEvent.builder()
                .userId(USER_ID)
                .activityId(activityId)
                .userSiteId(userSiteId)
                .startYearMonth(startYearMonth)
                .endYearMonth(endYearMonth)
                .time(ZonedDateTime.now(clock))
                .build();
    }

    public static TransactionsEnrichmentFinishedEvent createTransactionsEnrichmentFinishedEvent(final UUID activityId, final List<UserSiteInfo> userSiteInfo, final TransactionsEnrichmentFinishedEvent.Status status) {
        return new TransactionsEnrichmentFinishedEvent(USER_ID, activityId, userSiteInfo, ZonedDateTime.now(), status);
    }
}
