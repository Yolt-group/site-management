package nl.ing.lovebird.sitemanagement.usersiteevent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class UserSiteEventUpdate extends UserSiteEventAbstract {
    public UserSiteEventUpdate(
            @NonNull @JsonProperty("userSiteId") final UUID userSiteId,
            @NonNull @JsonProperty("userId") final UUID userId,
            @NonNull @JsonProperty("siteId") final UUID siteId,
            @NonNull @JsonProperty("time") final ZonedDateTime time) {
        super(userSiteId, userId, siteId, time, EventType.UPDATE_USER_SITE);
    }
}
