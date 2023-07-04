package nl.ing.lovebird.sitemanagement.health;

import com.vladmihalcea.hibernate.type.array.UUIDArrayType;
import lombok.*;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.StartEvent;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.health.Activity.TABLE;

/**
 * A database representation of an activity.
 * An activity is created when we receive the a {@link StartEvent}.
 * When an activity has a {@link Activity#endTime} that is not null, we consider the activity to be finished.
 * If the activity has a {@link Activity#endTime} that is null, we consider the activity to still be running.
 * <p>
 * Activities only exist in our database for 7 days, see YCO-1431.
 */
@Entity
@Table(name = TABLE)
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
        @TypeDef(
                name = "uuid-array",
                typeClass = UUIDArrayType.class
        )
})
public class Activity {
    public static final String TABLE = "activity";
    public static final String ACTIVITY_ID_COLUMN = "id";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String START_TIME_COLUMN = "start_time";
    public static final String END_TIME_COLUMN = "end_time";
    public static final String USER_SITE_IDS_COLUMN = "user_site_ids";
    public static final String START_EVENT_TYPE_COLUMN = "start_event_type";

    @Id
    @Column(name = ACTIVITY_ID_COLUMN, updatable = false, nullable = false)
    @NonNull
    private UUID activityId;

    @Column(name = USER_ID_COLUMN, updatable = false, nullable = false)
    @NonNull
    private UUID userId;

    @Column(name = START_TIME_COLUMN, updatable = false, nullable = false)
    @NonNull
    private Instant startTime;

    @Column(name = END_TIME_COLUMN)
    @Nullable
    private Instant endTime;

    @Column(name = START_EVENT_TYPE_COLUMN)
    @Enumerated(EnumType.STRING)
    @NonNull
    private EventType startEventType;

    @NonNull
    @Type(type = "uuid-array")
    @Column(name = USER_SITE_IDS_COLUMN, nullable = false, columnDefinition = "uuid[]")
    private UUID[] userSiteIds;
}
