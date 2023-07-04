package nl.ing.lovebird.sitemanagement.accountsandtransactions;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A helper object that, for a given user-site, contains the timestamp at which the next transaction retrieval should start.
 */
@Value
public class UserSiteTransactionStatusSummary {
    @NonNull
    UUID userSiteId;
    @NonNull
    Optional<Instant> transactionRetrievalLowerBoundTimestamp;
}
