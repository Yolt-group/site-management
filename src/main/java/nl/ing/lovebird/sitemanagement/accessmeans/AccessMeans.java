package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Access means are 'secret data' with which Yolt can retrieve data on behalf of an end user from a bank via a
 * 3rd party scraping provider.
 * (Yolt --> 3rd party scraping company --> bank)
 * <p>
 * This entity has a one-to-many correspondence with a {@link UserSite}.  A user can have n user-sites linked to
 * the same 3rd party scraping provider.  They all share the same AccessMeans.
 * <p>
 * See also: {@link UserSiteAccessMeans}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = AccessMeans.TABLE_NAME)
public class AccessMeans {

    public static final String TABLE_NAME = "access_means";

    public static final String USER_ID_COLUMN = "user_id";
    public static final String PROVIDER_COLUMN = "provider";
    public static final String ACCESS_MEANS_COLUMN = "access_means";
    public static final String UPDATED_COLUMN = "updated";
    public static final String EXPIRE_TIME_COLUMN = "expire_time";

    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @ClusteringColumn
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
}
