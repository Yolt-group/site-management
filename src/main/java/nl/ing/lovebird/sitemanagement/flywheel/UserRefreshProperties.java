package nl.ing.lovebird.sitemanagement.flywheel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Data
@ConfigurationProperties(prefix = "lovebird.flywheel.internal")
class UserRefreshProperties {
    private int defaultRefreshesPerDay;
    private Map<UUID, Integer> refreshesPerDay;
}
