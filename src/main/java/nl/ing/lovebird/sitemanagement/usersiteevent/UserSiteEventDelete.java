package nl.ing.lovebird.sitemanagement.usersiteevent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
public class UserSiteEventDelete extends UserSiteEventAbstract {

    public UserSiteEventDelete(
            @NonNull  @JsonProperty("userSiteId") final UUID userSiteId,
            @NonNull @JsonProperty("userId") final UUID userId,
            @NonNull @JsonProperty("siteId") final UUID siteId,
            @NonNull @JsonProperty("time") final ZonedDateTime time) {
        super(userSiteId, userId, siteId, time, EventType.DELETE_USER_SITE);
    }

}
