package nl.ing.lovebird.sitemanagement.health;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends CrudRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findAllByActivityIdOrderByEventTimeAsc(UUID activityId);

    @Modifying
    @Query(nativeQuery = true, value = "delete from activity where start_time < now() - cast(?1 || ' days' as interval)")
    int deleteActivitiesOlderThanDays(int days);
}
