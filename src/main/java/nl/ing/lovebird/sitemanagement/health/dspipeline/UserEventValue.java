package nl.ing.lovebird.sitemanagement.health.dspipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * We are interested only the the headers so that we can distinguish between create, update or delete based on message type.
 * The actual user data are be retrieved from user-context header.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEventValue {
    private final Headers headers;
}
