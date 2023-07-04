package nl.ing.lovebird.sitemanagement.flywheel;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@Slf4j
public class InternalFlywheelProperties {

    @Value("${lovebird.flywheel.internal.enabled}")
    private boolean enabled;

    @Value("${lovebird.flywheel.internal.throttling.minimumSecondsSinceLastRefresh}")
    private int minimumSecondsSinceLastRefresh;
    @Value("#{'${lovebird.flywheel.internal.throttling.blacklistedProviders:}'.replace(' ', '').split(',')}")
    private List<String> blacklistedProviders;
}
