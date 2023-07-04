package nl.ing.lovebird.sitemanagement.health.dspipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefreshPeriod {
    private final String startYearMonth;
    private final String endYearMonth;
}
