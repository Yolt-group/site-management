package nl.ing.lovebird.sitemanagement.usersite;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.time.Period.*;

/**
 * Compute a from-date (t) for a given UserSite that is used to request all transactions with a date >= t from providers.
 * <p>
 * There is a cut-off point (a date in the past) beyond which we are not interested in transactions for a particular
 * account.  This class contains functions to compute this date.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LastFetchedService {
    // Yodlee always crawls the last 40 days, so we do this as well....
    static final int OVERLAP_TRANSACTION_PERIOD_IN_DAYS = 40;

    private final Clock clock;

    public Instant determineTransactionRetrievalLowerBoundTimestamp(
            @NonNull ClientId clientId,
            Optional<Instant> userSiteLastDataFetch,
            Optional<Instant> userSiteAccessMeansCreated
    ) {
        return determineTransactionRetrievalLowerBoundTimestamp(clientId, userSiteLastDataFetch, userSiteAccessMeansCreated, Optional.empty(), Optional.empty());
    }

    public Instant determineTransactionRetrievalLowerBoundTimestamp(
            @NonNull ClientId clientId,
            Optional<Instant> userSiteLastDataFetch,
            Optional<Instant> userSiteAccessMeansCreated,
            Optional<Instant> transactionsDerivedLowerBound,
            Optional<String> provider
    ) {
        // We never go further back than lowerBound.
        final var lowerBound = ZonedDateTime.now(clock).minus(determineMaximumHistory(clientId)).toInstant();
        // We always ask for at least some time determined by upperBound.
        final var upperBound = Instant.now(clock).minus(determineUpperBound(clientId, provider));

        final boolean isDirectConnection = userSiteAccessMeansCreated.isPresent();
        // Return value.
        final Instant transactionRetrievalFrom;

        // Special case for direct connections: first data fetch ever, or first data fetch after a re-consent.
        //
        // If the access means have been created after the most recent data fetch then the consent might have changed,
        // e.g.: there could be a different set of accounts that we may retrieve from the bank.  Therefore, we retrieve
        // the full history.
        if (isDirectConnection && (userSiteLastDataFetch.isEmpty() || userSiteAccessMeansCreated.get().isAfter(userSiteLastDataFetch.get()))) {
            log.debug("determineTransactionRetrievalLowerBoundTimestamp is lowerBound (direct connection).");
            transactionRetrievalFrom = lowerBound;
        } else if (transactionsDerivedLowerBound.isPresent()) {
            // Take advice from summary
            log.debug("determineTransactionRetrievalLowerBoundTimestamp is derived value from accounts & transactions.");
            transactionRetrievalFrom = transactionsDerivedLowerBound.get();
        } else if (userSiteLastDataFetch.isPresent() && !Instant.EPOCH.equals(userSiteLastDataFetch.get())) {
            // We apparently have no trx data.  Fall back to the 'old' way of doing things and subtract X days.
            log.debug("determineTransactionRetrievalLowerBoundTimestamp is fixed period.");
            transactionRetrievalFrom = userSiteLastDataFetch.get().minus(OVERLAP_TRANSACTION_PERIOD_IN_DAYS, ChronoUnit.DAYS);
        } else {
            // Fall back to retrieving the full history.  In practice we should never get here.
            // XXX Turn this warning into an IllegalArgumentException if it turns out that this never happens.
            log.warn("determineTransactionRetrievalLowerBoundTimestamp is falling back to lowerBound, should not happen.");
            transactionRetrievalFrom = lowerBound;
        }

        // Make sure we never ask for anything prior to lowerBound or after upperBound.
        if (transactionRetrievalFrom.isBefore(lowerBound)) {
            return lowerBound;
        } else if (transactionRetrievalFrom.isAfter(upperBound)) {
            return upperBound;
        }

        return transactionRetrievalFrom;
    }

    /**
     * Computes the maximum history of transactions that we request from the bank.  This time duration is typically
     * used during the first time we fetch data at a bank to grab the transaction history for a user.  Subsequent
     * data fetches will ask for a smaller timeframe.
     */
    private static Period determineMaximumHistory(@NonNull ClientId clientId) {
        if (ClientIds.ACCOUNTING_CLIENT.equals(clientId) || ClientIds.YTS_CREDIT_SCORING_APP.equals(clientId)) {
            // Both want 18 months of data.
            return ofMonths(18);
        }

        // Default for clients that do not specify this is one year.
        return ofYears(1);
    }

    private static Period determineUpperBound(@NonNull ClientId clientId, Optional<String> provider) {
        // We want to fetch 30 days of history for triodos and rabobank user-sites to ensure we clean up old duplicates
        Period upperBound = provider.filter(__ -> ClientIds.ACCOUNTING_CLIENT.equals(clientId))
                .filter(p -> "TRIODOS_BANK_NL".equals(p) || "RABOBANK".equals(p))
                .map(__ -> ofDays(30))
                .orElse(ofDays(21));

        if (provider.isPresent() && provider.get().equals("RABOBANK") && !ClientIds.ACCOUNTING_CLIENT.equals(clientId)) {
            upperBound = ofDays(60);
        }
        return upperBound;
    }
}
