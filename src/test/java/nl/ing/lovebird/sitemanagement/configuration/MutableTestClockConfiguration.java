package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.sitemanagement.lib.MutableClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MutableTestClockConfiguration {

    @Bean
    public MutableClock clock() {
        return new MutableClock();
    }
}
