package nl.ing.lovebird.sitemanagement.externalconsent;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = ExternalConsent.TABLE_NAME)
@Validated
public class ExternalConsent {

    static final String TABLE_NAME = "external_consent_v3";

    static final String USER_ID_COLUMN = "user_id";
    static final String SITE_ID_COLUMN = "site_id";
    static final String USER_SITE_ID_COLUMN = "user_site_id";
    static final String EXPIRY_TIMESTAMP_COLUMN = "expiry_timestamp";
    static final String EXPIRY_WEEK_COLUMN = "expiry_week";
    static final String CONSENT_TIMESTAMP_COLUMN = "consent_timestamp";
    static final String EXTERNAL_CONSENT_ID_COLUMN = "external_consent_id";

    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    @NotNull
    private UUID userId;

    @ClusteringColumn
    @Column(name = SITE_ID_COLUMN)
    @NotNull
    private UUID siteId;

    @ClusteringColumn(1)
    @Column(name = USER_SITE_ID_COLUMN)
    @NotNull
    private UUID userSiteId;

    /**
     * This column is used to divide the set of external consents into initial buckets because cassandra can only search
     * by primary key on equality (i.e. no searching for less than or greater than the timestamp).
     * Further querying can then be done on {@link #expiryTimestamp} within the bucket.
     * Week is chosen, because that would allow around 12M user sites to store their consents with an even spread around the year.
     * Buckets of months is too large because of this (soft limit for Cassandra partitions is 50MB).
     */
    @Column(name = EXPIRY_WEEK_COLUMN)
    @NotNull
    private String expiryWeek;

    @Column(name = EXPIRY_TIMESTAMP_COLUMN)
    @NotNull
    private Instant expiryTimestamp;

    @Column(name = CONSENT_TIMESTAMP_COLUMN)
    @NotNull
    private Instant consentTimestamp;

    @Column(name = EXTERNAL_CONSENT_ID_COLUMN)
    @Nullable
    private String externalConsentId;


}
