package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.sitemanagement.health.dspipeline.RefreshPeriod;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.health.activities.ActivityEventTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

public class ActivityDoneCheckerTest {

    private final static UUID ACTIVITY_ID = UUID.randomUUID();
    private static final RefreshPeriod refreshPeriod = new RefreshPeriod("2018-06", "2018-07");

    @Test
    public void isActivityDone_oneUserSiteDone() {
        UUID userSiteId = UUID.randomUUID();

        AbstractEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId);
        AbstractEvent ingestionFinishedEvent1 = createIngestionFinishedEvent(ACTIVITY_ID, userSiteId, refreshPeriod);

        List<AbstractEvent> abstractEvents = Arrays.asList(
                startEvent,
                ingestionFinishedEvent1
        );

        boolean activityDone = ActivityDoneChecker.isLastProcessedUserSite(abstractEvents, ingestionFinishedEvent1);

        assertTrue(activityDone);
    }

    @Test
    public void isActivityDone_oneUserSiteRefreshedEventReceived() {
        UUID userSiteId = UUID.randomUUID();

        AbstractEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId);
        AbstractEvent siteManagementEndEvent = createRefreshedEvent(ACTIVITY_ID, userSiteId);

        List<AbstractEvent> activityEvents = Arrays.asList(
                startEvent,
                siteManagementEndEvent
        );

        boolean activityDone = ActivityDoneChecker.isLastProcessedUserSite(activityEvents, siteManagementEndEvent);

        assertTrue(activityDone);
    }

    @Test
    public void isActivityDone_oneUserSiteOnlyStarted() {
        UUID userSiteId = UUID.randomUUID();

        AbstractEvent event = createStartEvent(ACTIVITY_ID, userSiteId);

        List<AbstractEvent> activityEvents = Collections.singletonList(
                event
        );

        boolean activityDone = ActivityDoneChecker.isLastProcessedUserSite(activityEvents, event);

        assertFalse(activityDone);
    }

    @Test
    public void isActivityDone_twoUserSitesOnlyStarted() {
        UUID userSiteId1 = UUID.randomUUID();
        UUID userSiteId2 = UUID.randomUUID();

        AbstractEvent event = createStartEvent(ACTIVITY_ID, userSiteId1, userSiteId2);

        List<AbstractEvent> activityEvents = Collections.singletonList(
                event
        );

        boolean activityDone = ActivityDoneChecker.isLastProcessedUserSite(activityEvents, event);

        assertFalse(activityDone);
    }

    @Test
    public void isActivityDone_twoUserSitesOneDone() {
        UUID userSiteId1 = UUID.randomUUID();
        UUID userSiteId2 = UUID.randomUUID();


        AbstractEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId1, userSiteId2);
        AbstractEvent ingestionFinishedEvent1 = createIngestionFinishedEvent(ACTIVITY_ID, userSiteId1, refreshPeriod);

        List<AbstractEvent> activityEvents = Arrays.asList(
                startEvent,
                ingestionFinishedEvent1
        );

        boolean activityDone = ActivityDoneChecker.isLastProcessedUserSite(activityEvents, ingestionFinishedEvent1);

        assertFalse(activityDone);
    }

    @Test
    public void isActivityDone_twoStartEvents_OK_idempotent() {
        UUID userSiteId1 = UUID.randomUUID();
        UUID userSiteId2 = UUID.randomUUID();

        AbstractEvent event1 = createStartEvent(ACTIVITY_ID, userSiteId1);
        AbstractEvent event2 = createStartEvent(ACTIVITY_ID, userSiteId2);

        List<AbstractEvent> activityEvents = Arrays.asList(
                event1,
                event2
        );

        assertFalse(ActivityDoneChecker.isLastProcessedUserSite(activityEvents, event2));
    }

    @Test
    public void isActivityDone_twoRefreshedEvents_OK_idempotent() {
        UUID userSiteId1 = UUID.randomUUID();

        AbstractEvent startEvent = createStartEvent(ACTIVITY_ID, userSiteId1);
        AbstractEvent refreshedEvent1 = createRefreshedEvent(ACTIVITY_ID, userSiteId1);
        AbstractEvent refreshedEvent2 = createRefreshedEvent(ACTIVITY_ID, userSiteId1);
        IngestionFinishedEvent ingestionFinishedEvent = createIngestionFinishedEvent(ACTIVITY_ID, userSiteId1, new RefreshPeriod(null, null));
        List<AbstractEvent> activityEvents = Arrays.asList(
                startEvent,
                refreshedEvent1,
                refreshedEvent2,
                ingestionFinishedEvent);

        assertTrue(ActivityDoneChecker.isLastProcessedUserSite(activityEvents, ingestionFinishedEvent));
    }

    @Test
    public void isActivityDone_missingStartEvent() {
        UUID userSiteId1 = UUID.randomUUID();

        AbstractEvent refreshedEvent = createRefreshedEvent(ACTIVITY_ID, userSiteId1);

        List<AbstractEvent> activityEvents = Collections.singletonList(
                refreshedEvent
        );

        assertThrows(IllegalActivityStateException.class, () -> ActivityDoneChecker.isLastProcessedUserSite(activityEvents, refreshedEvent));
    }
}
