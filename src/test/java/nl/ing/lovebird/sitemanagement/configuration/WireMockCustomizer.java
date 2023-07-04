package nl.ing.lovebird.sitemanagement.configuration;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockCustomizer implements WireMockConfigurationCustomizer {

    @Override
    public void customize(WireMockConfiguration config) {
        config.extensions(new ResponseTemplateTransformer(true));
    }
}
