package nl.ing.lovebird.sitemanagement.accessmeans;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@IntegrationTestContext
@TestPropertySource(properties = {
        "yolt.site-management.custom-expired-consent-flow.applicable-client-ids=" + CustomExpiredConsentFlowConfigurationIntegrationTest.DUMMY_CLIENT_ID,
        "yolt.site-management.custom-expired-consent-flow.applicable-site-ids=" + CustomExpiredConsentFlowConfigurationIntegrationTest.DUMMY_SITE_ID,
        "yolt.site-management.custom-expired-consent-flow.minimum-consent-age-before-disconnect=" + CustomExpiredConsentFlowConfigurationIntegrationTest.DUMMY_DURATION})
class CustomExpiredConsentFlowConfigurationIntegrationTest {

    static final String DUMMY_SITE_ID = "198f41a3-0018-4a42-82fa-23d96d7e62f9";
    static final String DUMMY_CLIENT_ID = "e9f144cd-905e-4d64-a406-b71fce733f9f";
    static final String DUMMY_DURATION = "P3D";

    @Autowired
    private CustomExpiredConsentFlowConfiguration customExpiredConsentFlowConfiguration;

    @Test
    void configurationReturnsExpectedConfiguration() {
        assertThat(customExpiredConsentFlowConfiguration.getApplicableSiteIds()).containsExactly(UUID.fromString(DUMMY_SITE_ID));
        assertThat(customExpiredConsentFlowConfiguration.getApplicableClientIds()).containsExactly(UUID.fromString(DUMMY_CLIENT_ID));
        assertThat(customExpiredConsentFlowConfiguration.getMinimumConsentAgeBeforeDisconnect()).isEqualByComparingTo(Duration.parse(DUMMY_DURATION));
    }
}
