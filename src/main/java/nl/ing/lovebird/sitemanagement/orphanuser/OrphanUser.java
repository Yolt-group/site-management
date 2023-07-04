package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.time.Instant;
import java.util.UUID;

/**
 * This entity represents a user that exists in external provider (for example, SaltEdge) but was already deleted from our system.
 * We run user list comparison (between external provider and our system) from time to time and call single diff
 * result a 'batch' {@link OrphanUserBatch}, that is why every orphaned user entry has a orphanUserBatchId and any
 * orphaned user may appear in several different batches
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = OrphanUser.TABLE_NAME)
public class OrphanUser {

    public static final String TABLE_NAME = "orphan_user";

    public static final String PROVIDER_COLUMN = "provider";
    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String ORPHAN_USER_BATCH_ID_COLUMN = "orphan_user_batch_id";
    public static final String EXTERNAL_USER_ID = "external_user_id";
    public static final String CREATED_TIMESTAMP_COLUMN = "created_timestamp";
    public static final String UPDATED_TIMESTAMP_COLUMN = "updated_timestamp";
    public static final String STATUS_COLUMN = "status";

    public enum Status {
        INITIAL, // upon first encounter
        ERROR, // if removing at external provider failed
        DELETED // if removing at external provider succeeded
    }

    @PartitionKey
    @Column(name = CLIENT_ID_COLUMN)
    private ClientId clientId;

    @ClusteringColumn(0)
    @Column(name = PROVIDER_COLUMN)
    private String provider;

    @ClusteringColumn(1)
    @Column(name = ORPHAN_USER_BATCH_ID_COLUMN)
    private UUID orphanUserBatchId;

    @ClusteringColumn(2)
    @Column(name = EXTERNAL_USER_ID)
    private String externalUserId;

    @Column(name = CREATED_TIMESTAMP_COLUMN)
    private Instant createdTimestamp;

    @Column(name = UPDATED_TIMESTAMP_COLUMN)
    private Instant updatedTimestamp;

    @Column(name = STATUS_COLUMN)
    private Status status;
}
