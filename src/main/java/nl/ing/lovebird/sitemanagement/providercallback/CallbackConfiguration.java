package nl.ing.lovebird.sitemanagement.providercallback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix="callbacks")
public class CallbackConfiguration {

    private final Map<String, String> userIdJsonPathExpressions;

}
