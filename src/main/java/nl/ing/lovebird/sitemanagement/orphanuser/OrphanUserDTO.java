package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class OrphanUserDTO {

    @NonNull UUID orphanUserBatchId;
    @NonNull String provider;
    @NonNull String externalUserId;
    @NonNull Instant createdTimestamp;
    @NonNull Instant updatedTimestamp;
    @NonNull String status;
}
