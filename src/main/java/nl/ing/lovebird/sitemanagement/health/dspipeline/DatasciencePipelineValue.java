package nl.ing.lovebird.sitemanagement.health.dspipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasciencePipelineValue {
    private final Headers headers;
    private final DatasciencePipelinePayload payload;
}
