package nl.ing.lovebird.sitemanagement.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import nl.ing.lovebird.activityevents.events.AbstractEvent;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.health.ActivityEvent.TABLE;

/**
 * Postgres representation of an event related to an activity.
 * The raw {@link AbstractEvent} json is stored in the database along with other relevant information for the event.
 * <p>
 * With regards to storing the raw json in postgres:
 * This is achieved by relying on hibernate to serialize and deserialize the column data to and from json.
 * There are multiple ways to represent the json column on the entity (i.e. use a {@link String}, {@link JsonNode} or the POJO itself).
 * Using the {@link String} and {@link JsonNode} comes with the benefit of being able to make the serialization and deserialzation
 * resilient to (breaking) schema changes. But they come with the drawback of having to write the serialization and deserialzation
 * manually.
 * <p>
 * Using the POJO itself comes with the benefit showing the exact datastructure of the persisted json when looking at this
 * entity and we also do not have to write the serialization and deserialization manually.
 * <p>
 * We chose to use the POJO itself in this case because breaking changes will not occur for the {@link AbstractEvent}.
 * This POJO is used in all flows within Yolt and it is safe to assume only non-breaking changes are introduced when
 * updating the POJO.
 */
@Entity
@Table(name = TABLE)
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs(@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class))
public class ActivityEvent {
    public static final String TABLE = "activity_events";
    public static final String EVENT_ID_COLUMN = "event_id";
    public static final String ACTIVITY_ID_COLUMN = "activity_id";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String EVENT_COLUMN = "event";
    public static final String EVENT_TIME_COLUMN = "event_time";

    @Id
    @Column(name = EVENT_ID_COLUMN, updatable = false, nullable = false)
    @NonNull
    private UUID eventId;

    @Column(name = ACTIVITY_ID_COLUMN, updatable = false, nullable = false)
    @NonNull
    private UUID activityId;

    @Column(name = USER_ID_COLUMN, updatable = false, nullable = false)
    @NonNull
    private UUID userId;

    @Type(type = "jsonb")
    @Column(name = EVENT_COLUMN, columnDefinition = "jsonb", nullable = false)
    @NonNull
    private AbstractEvent event;

    @Column(name = EVENT_TIME_COLUMN, nullable = false)
    @NonNull
    private Instant eventTime;
}
