package nl.ing.lovebird.sitemanagement.externalconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsentExpiryTimeFormatterTest {

    @Test
    void testFormatInstantEpoch() {

        Instant input = Instant.EPOCH;
        assertThat(ConsentExpiryTimeFormatter.format(input)).isEqualTo("1970-01");
    }

    @Test
    void testFormatInstantDecemberAfterEpoch() {

        Instant input = Instant.parse("1970-12-31T00:00:00.00Z");
        assertThat(ConsentExpiryTimeFormatter.format(input)).isEqualTo("1970-53");
    }
}