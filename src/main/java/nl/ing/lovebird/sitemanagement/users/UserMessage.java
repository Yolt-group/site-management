package nl.ing.lovebird.sitemanagement.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
public record UserMessage(@NotNull @Valid Headers headers,
                          @Valid Payload payload) {

    public record Headers(@NotNull String messageType) {
    }

    public record Payload(UUID id,
                   ClientId clientId,
                   UserMessageType userMessageType,
                   boolean oneOffAis,
                   ZonedDateTime lastLogin) {

        public enum UserMessageType {
            USER_CREATED,
            USER_LOGIN,
            USER_UPDATED,
            USER_DELETED,
            UNKNOWN;

            @JsonCreator
            public static UserMessageType fromValue(String value) {
                try {
                    return UserMessageType.valueOf(value);
                } catch (IllegalArgumentException e) {
                    log.warn("Could not deserialize message type {}. Falling back to unknown.", value);
                    return UNKNOWN;
                }
            }
        }
    }
}