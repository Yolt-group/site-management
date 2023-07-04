package nl.ing.lovebird.sitemanagement.health.dspipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Headers {
    private final String messageType;
    private final UUID userId;
    /**
     * @deprecated tracing is handled by sleuth. This requestTraceId should not be read.
     */
    @Deprecated(forRemoval = true)
    private final UUID requestTraceId;
}
