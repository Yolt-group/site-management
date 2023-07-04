package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.UserSiteStartEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
final class ActivityDoneChecker {

    private ActivityDoneChecker() {
    }

    static boolean isLastProcessedUserSite(final List<AbstractEvent> abstractEvents, AbstractEvent currentEvent) {

        Set<UUID> userSitesInActivity = getDistinctEvents(abstractEvents, UserSiteStartEvent.class)
                .stream()
                .flatMap(it -> it.getUserSiteIds().stream())
                .collect(Collectors.toSet());
        if (userSitesInActivity.isEmpty()) {
            // Error, because this is some weird race condition. No start event, while finished event.
            throw new IllegalActivityStateException("Activity " + currentEvent.getActivityId() + " not ready yet. Got "
                    + currentEvent.getClass().getSimpleName() + " while there are no expected user sites yet.");
        }

        // Check if we have a 'ingestion finished' event (OK) or 'refresh finished FAILED' (KO) for each expected usersite.
        Set<UUID> ingestedUserSites = getDistinctEvents(abstractEvents, IngestionFinishedEvent.class).stream()
                .map(IngestionFinishedEvent::getUserSiteId)
                .collect(Collectors.toSet());

        Set<UUID> failedUserSites = getDistinctEvents(abstractEvents, RefreshedUserSiteEvent.class).stream()
                .map(RefreshedUserSiteEvent::getUserSiteId)
                .collect(Collectors.toSet());

        Set<UUID> allProcessedUserSites = new HashSet<>(ingestedUserSites);
        allProcessedUserSites.addAll(failedUserSites);

        if (allProcessedUserSites.containsAll(userSitesInActivity)) {
            log.info("Activity {} is done. Ingested user sites {}, failed user sites {}.",
                    currentEvent.getActivityId(),
                    ingestedUserSites,
                    failedUserSites);
            return true;
        } else {
            userSitesInActivity.removeAll(ingestedUserSites);
            log.info("Activity {} is not done yet. Waiting for ingestion of usersites {}",
                    currentEvent.getActivityId(),
                    userSitesInActivity);
           return false;
        }
    }

    private static <T> Set<T> getDistinctEvents(final List<AbstractEvent> abstractEvents, Class<T> clazz) {
        //noinspection unchecked
        return abstractEvents.stream()
                .filter(clazz::isInstance)
                .map(it -> (T) it)
                .collect(Collectors.toSet());
    }
}
