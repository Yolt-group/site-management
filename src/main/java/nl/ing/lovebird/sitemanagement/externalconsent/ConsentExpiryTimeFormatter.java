package nl.ing.lovebird.sitemanagement.externalconsent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ConsentExpiryTimeFormatter {

    private static final String CONSENT_EXPIRY_WEEK_PATTERN = "yyyy-ww";
    private static final DateTimeFormatter EXPIRY_TIME_FORMATTER = DateTimeFormatter
            .ofPattern(CONSENT_EXPIRY_WEEK_PATTERN)
            .withZone(ZoneOffset.UTC)
            .withLocale(Locale.GERMANY);

    static String format(Instant instant) {
        return EXPIRY_TIME_FORMATTER.format(instant);
    }
}
