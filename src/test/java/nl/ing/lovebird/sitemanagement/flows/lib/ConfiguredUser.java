package nl.ing.lovebird.sitemanagement.flows.lib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.users.User;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A user that the system knows exists.
 */
@Getter
@RequiredArgsConstructor
public class ConfiguredUser {

    final User user;

    public UUID getId() {
        return user.getUserId();
    }

    public String getPsuIpAddress() {
        return "192.168.0.1";
    }

}