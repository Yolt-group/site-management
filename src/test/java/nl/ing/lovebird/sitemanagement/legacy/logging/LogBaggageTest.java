package nl.ing.lovebird.sitemanagement.legacy.logging;

import nl.ing.lovebird.logging.MDCContextCreator;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LogBaggageTest {

    @Test
    void sanityCheck() {
        try (LogBaggage b = LogBaggage.builder().provider("YOLT_PROVIDER").build()) {
            assertThat(MDC.get("provider")).isEqualTo("YOLT_PROVIDER");
        }
        assertThat(MDC.get("provider")).isNull();
    }

    @Test
    void nestedUsageCheck() {
        try (LogBaggage b1 = LogBaggage.builder().provider("YOLT_PROVIDER").build()) {
            UUID siteId = UUID.randomUUID();
            try (LogBaggage b2 = LogBaggage.builder().siteId(siteId).build()) {
                // Nested usage shouldn't nuke previous values.
                assertThat(MDC.get("provider")).isEqualTo("YOLT_PROVIDER");

                // New value should be present.
                assertThat(MDC.get(MDCContextCreator.SITE_ID_MDC_KEY)).isEqualTo(siteId.toString());
            }
            // Nested value should be gone.
            assertThat(MDC.get(MDCContextCreator.SITE_ID_MDC_KEY)).isNull();
        }
        // Everything should be gone.
        assertThat(MDC.get("provider")).isNull();
    }

}
