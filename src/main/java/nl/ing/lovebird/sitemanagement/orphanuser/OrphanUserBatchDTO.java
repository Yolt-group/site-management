package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class OrphanUserBatchDTO implements Comparable<OrphanUserBatchDTO> {

    @NonNull UUID orphanUserBatchId;
    @NonNull String provider;
    @NonNull Instant createdTimestamp;
    @NonNull Instant updatedTimestamp;
    @NonNull String status;
    boolean executionAllowed;

    @Override
    public int compareTo(OrphanUserBatchDTO other) {
        return other.updatedTimestamp.compareTo(this.updatedTimestamp);
    }
}
