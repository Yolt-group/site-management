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
 * This entity represents a list of orphaned users {@link OrphanUser} obtained in one comparison of user lists between
 * external provider and our system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = OrphanUserBatch.TABLE_NAME)
public class OrphanUserBatch {

    public static final String TABLE_NAME = "orphan_user_batch";

    public static final String PROVIDER_COLUMN = "provider";
    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String ORPHAN_USER_BATCH_ID_COLUMN = "orphan_user_batch_id";
    public static final String CREATED_TIMESTAMP_COLUMN = "created_timestamp";
    public static final String UPDATED_TIMESTAMP_COLUMN = "updated_timestamp";
    public static final String STATUS_COLUMN = "status";

    public enum Status {
        // Batch preparation steps
        PREPARE_INITIATED, // triggered preparation phase
        PREPARE_RECEIVING_DATA, // getting data chunks from Kafka
        PREPARE_RECEIVING_DATA_FINISHED, // all data from Kafka received (last message received)
        PREPARE_PROCESSING, // started finding orphaned users for provider
        PREPARE_PROCESSING_FINISHED, // finished finding orphaned users for provider

        // Batch execution steps
        EXECUTE_INITIATED, // triggered execution phase
        EXECUTE_EMPTY, // no orphaned users found at provider for this batch
        EXECUTE_FINISHED_SUCCESS, // all orphaned users were successfully deleted at provider
        EXECUTE_FINISHED_WITH_ERRORS // at least one orphaned user deletion failed at provider
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

    @Column(name = CREATED_TIMESTAMP_COLUMN)
    private Instant createdTimestamp;

    @Column(name = UPDATED_TIMESTAMP_COLUMN)
    private Instant updatedTimestamp;

    @Column(name = STATUS_COLUMN)
    private OrphanUserBatch.Status status;
}
