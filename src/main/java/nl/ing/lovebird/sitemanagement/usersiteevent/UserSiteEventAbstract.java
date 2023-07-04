package nl.ing.lovebird.sitemanagement.usersiteevent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.usersiteevent.EventType;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventDelete;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventUpdate;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserSiteEventUpdate.class, name = "UPDATE_USER_SITE"),
        @JsonSubTypes.Type(value = UserSiteEventDelete.class, name = "DELETE_USER_SITE"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class UserSiteEventAbstract {
    private final UUID userSiteId;
    private final UUID userId;
    private final UUID siteId;
    private final ZonedDateTime time;
    private final EventType type;

}
