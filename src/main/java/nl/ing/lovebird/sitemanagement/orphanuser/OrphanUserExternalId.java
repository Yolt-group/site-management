package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.util.UUID;

/**
 * Represents external user ids list for a certain provider, aggregated within single orphaned users batch run.
 * This data is temporal - as soon as orphaned users batch for a certain provider will be executed, all corresponding
 * external user ids would be removed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = OrphanUserExternalId.TABLE_NAME)
public class OrphanUserExternalId {

    public static final String TABLE_NAME = "orphan_user_external_id";

    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String PROVIDER_COLUMN = "provider";
    public static final String ORPHAN_USER_BATCH_ID_COLUMN = "orphan_user_batch_id";
    public static final String EXTERNAL_USER_ID = "external_id";

    @PartitionKey
    @Column(name = CLIENT_ID_COLUMN)
    private ClientId clientId;

    @ClusteringColumn(1)
    @Column(name = PROVIDER_COLUMN)
    private String provider;

    @ClusteringColumn(0)
    @Column(name = ORPHAN_USER_BATCH_ID_COLUMN)
    private UUID orphanUserBatchId;

    @ClusteringColumn(2)
    @Column(name = EXTERNAL_USER_ID)
    private String externalUserId;
}
