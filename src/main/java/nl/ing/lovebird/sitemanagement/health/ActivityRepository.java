package nl.ing.lovebird.sitemanagement.health;

import nl.ing.lovebird.activityevents.EventType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ActivityRepository extends CrudRepository<Activity, UUID> {

    @Transactional(readOnly = true)
    List<Activity> getAllByUserIdAndStartEventTypeIn(@NonNull UUID userId, Set<EventType> eventTypes);

    @Modifying
    @Query(nativeQuery = true, value = "delete from activity_events where event_time < now() - cast(?1 || ' days' as interval)")
    int deleteActivityEventsOlderThanDays(int days);
}
