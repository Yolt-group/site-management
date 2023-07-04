package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

public class LastFetchedServiceTest {

    Instant now = Instant.now();
    Instant tomorrow = now.plus(1, DAYS);
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    LastFetchedService systemUnderTest = new LastFetchedService(clock);

    /**
     * Test the fallback to "lastDataFetch - 40 days" if oldestPendingTrxDate and mostRecentBookedTrxDate are not present.
     */
    @Test
    void given_lastDataFetch_when_determineLowerBound_then_fallbackTo40DaysAgo() {
        var userSiteLastDataFetch = now;
        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.YTS_CREDIT_SCORING_APP, Optional.ofNullable(userSiteLastDataFetch),
                        Optional.empty());
        assertThat(fetchTransactionsStartInstant).isEqualTo(userSiteLastDataFetch.minus(40, DAYS));
    }

    /**
     * Check that the (per-client) lower bound is respected even if data was not fetched for more than one year (which
     * is the lower bound for the yolt app).
     */
    @Test
    void given_lastDataFetchOverAYearAgo_when_determineLowerBound_then_fetchMaxOneYear() {
        var userSiteLastDataFetch = now.minus(900, DAYS);
        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.YTS_CREDIT_SCORING_APP, Optional.ofNullable(userSiteLastDataFetch),
                        Optional.empty());

        assertThat(fetchTransactionsStartInstant).isEqualTo(OffsetDateTime.now(clock).minusMonths(18).toInstant());
    }

    /**
     * Test the case where we need to fetch the full history if the userSiteAccessMeans have been created more recently
     * than the lastDataFetch happened.
     */
    @Test
    void given_accessMeansRefreshedAfterLastDataFetch_when_determineLowerBound_then_fetchFullHistory() {
        var userSiteLastDataFetch = now;
        var userSiteAccessMeansCreated = tomorrow;
        assertThat(userSiteAccessMeansCreated).isAfter(userSiteLastDataFetch);

        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.YTS_CREDIT_SCORING_APP, Optional.ofNullable(userSiteLastDataFetch),
                        Optional.ofNullable(userSiteAccessMeansCreated));

        assertThat(fetchTransactionsStartInstant).isEqualTo(OffsetDateTime.now(clock).minusMonths(18).toInstant());
    }

    /**
     * If a transactions derived lower bound is present, use it.
     */
    @Test
    void given_transactionsDerivedLowerBoundOfNDaysAgo_when_determineLowerBound_then_fetchNDays() {
        int n = 50;
        var userSiteLastDataFetch = tomorrow;
        var userSiteAccessMeansCreated = now;
        var transactionsDerivedLowerBound = userSiteLastDataFetch.minus(n, DAYS);
        assertThat(n).isGreaterThan(LastFetchedService.OVERLAP_TRANSACTION_PERIOD_IN_DAYS);
        assertThat(userSiteLastDataFetch).isAfter(userSiteAccessMeansCreated);

        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.YTS_CREDIT_SCORING_APP, Optional.of(userSiteLastDataFetch),
                        Optional.of(userSiteAccessMeansCreated), Optional.of(transactionsDerivedLowerBound), Optional.empty());

        assertThat(fetchTransactionsStartInstant).isEqualTo(transactionsDerivedLowerBound);
    }

    /**
     * If the transactions derived lower bound is in the future, we need to fetch one day in the past.
     */
    @Test
    void given_transactionsDerivedLowerBoundOfNDaysInFuture_when_determineLowerBound_then_fetch21Days() {
        int n = 1;
        var userSiteLastDataFetch = tomorrow;
        var userSiteAccessMeansCreated = now;
        var transactionsDerivedLowerBound = userSiteLastDataFetch.plus(n, DAYS);
        assertThat(userSiteLastDataFetch).isAfter(userSiteAccessMeansCreated);

        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.YTS_CREDIT_SCORING_APP, Optional.of(userSiteLastDataFetch),
                        Optional.of(userSiteAccessMeansCreated), Optional.of(transactionsDerivedLowerBound), Optional.empty());

        assertThat(fetchTransactionsStartInstant).isEqualTo(now.minus(21, DAYS));
    }

    /**
     * Fetch up to a specific maximum number of days.
     */
    @Test
    void given_transactionsDerivedLowerBoundInFarPast_when_determineLowerBound_then_fetchMaximumDays() {
        var userSiteLastDataFetch = tomorrow;
        var userSiteAccessMeansCreated = now;
        var transactionsDerivedLowerBound = now.minus(1000, DAYS);
        assertThat(userSiteLastDataFetch).isAfter(userSiteAccessMeansCreated);

        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientId.random(), Optional.of(userSiteLastDataFetch),
                        Optional.of(userSiteAccessMeansCreated), Optional.of(transactionsDerivedLowerBound), Optional.empty());

        assertThat(fetchTransactionsStartInstant).isCloseTo(now.atZone(ZoneOffset.UTC).minus(1, YEARS).toInstant(), byLessThan(1, SECONDS));
    }

    /**
     * Test the client-dependent amount of history we fetch.
     */
    @Test
    void given_initialFetchForYolt_when_determineLowerBound_then_aYearIsUsed() {
        var fetchTransactionsStartInstant = systemUnderTest.determineTransactionRetrievalLowerBoundTimestamp(
                ClientIds.YTS_CREDIT_SCORING_APP,
                Optional.empty(),
                Optional.empty()
        );

        var expectedStartInstant = OffsetDateTime.now(clock).minusMonths(18).toInstant();
        assertThat(expectedStartInstant).isCloseTo(fetchTransactionsStartInstant, byLessThan(1, SECONDS));
    }

    /**
     * Test the client-dependent amount of history we fetch.
     */
    @Test
    void given_initialFetch_when_determineLowerBound_then_18MonthsAreUsed() {
        var fetchTransactionsStartInstant = systemUnderTest.determineTransactionRetrievalLowerBoundTimestamp(
                ClientIds.ACCOUNTING_CLIENT,
                Optional.empty(),
                Optional.empty()
        );

        var expectedStartInstant = OffsetDateTime.now(clock).minusMonths(18).toInstant();
        assertThat(expectedStartInstant).isCloseTo(fetchTransactionsStartInstant, byLessThan(1, SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRIODOS_BANK_NL", "RABOBANK"})
    void given_triodosFetch_whenDetermineUpperBound_then_30DaysAreUsed(String provider) {
        int n = 1;
        var userSiteLastDataFetch = tomorrow;
        var userSiteAccessMeansCreated = now;
        var transactionsDerivedLowerBound = userSiteLastDataFetch.plus(n, DAYS);
        assertThat(userSiteLastDataFetch).isAfter(userSiteAccessMeansCreated);

        var fetchTransactionsStartInstant = systemUnderTest
                .determineTransactionRetrievalLowerBoundTimestamp(ClientIds.ACCOUNTING_CLIENT, Optional.of(userSiteLastDataFetch),
                        Optional.of(userSiteAccessMeansCreated), Optional.of(transactionsDerivedLowerBound), Optional.of(provider));

        assertThat(fetchTransactionsStartInstant).isEqualTo(now.minus(30, DAYS));
    }
}
