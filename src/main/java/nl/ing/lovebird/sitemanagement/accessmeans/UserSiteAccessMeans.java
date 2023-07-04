package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Access means are 'secret data' with which Yolt can retrieve data on behalf of an end user directly from a bank.
 * (Yolt --> bank)
 * <p>
 * This entity has a one-to-one correspondence with a UserSite.
 * <p>
 * See also: {@link AccessMeans}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = UserSiteAccessMeans.TABLE_NAME)
public class UserSiteAccessMeans {

    public static final String TABLE_NAME = "user_site_access_means";

    public static final String USER_ID_COLUMN = "user_id";
    public static final String USER_SITE_ID_COLUMN = "user_site_id";
    public static final String PROVIDER_COLUMN = "provider";
    public static final String ACCESS_MEANS_COLUMN = "access_means";
    public static final String UPDATED_COLUMN = "updated";
    public static final String EXPIRE_TIME_COLUMN = "expire_time";
    public static final String CREATED_COLUMN = "created";

    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @ClusteringColumn
    @Column(name = USER_SITE_ID_COLUMN)
    private UUID userSiteId;

    @ClusteringColumn(1)
    @Column(name = PROVIDER_COLUMN)
    private String provider;

    /**
     * Encrypted.
     */
    @Column(name = ACCESS_MEANS_COLUMN)
    private String accessMeans;

    @Column(name = UPDATED_COLUMN)
    private Date updated;

    @Column(name = EXPIRE_TIME_COLUMN)
    private Date expireTime;

    /**
     * The timestamp at which the {@link UserSiteAccessMeans} were created.  This field is not a typical createdAt
     * timestamp that is set exactly once and is left alone afterwards.
     *
     * <p>This field has functional meaning and therefore it *must* be set to 'now' whenever new accessmeans are
     * retrieved from a site.  In other words: this field needs to be updated whenever the consent is created or
     * updated.  We communicate this field to clients to give them an indication of how much longer a consent
     * might be valid for at a site.
     */
    @Column(name = CREATED_COLUMN)
    private Instant created;

}
