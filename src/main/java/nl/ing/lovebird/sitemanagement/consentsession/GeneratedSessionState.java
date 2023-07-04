package nl.ing.lovebird.sitemanagement.consentsession;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * If we ever create a 'state' during the loginProcess that will either be served with the form, or embedded in a redirectUrl, it will
 * be saved here.
 * We'll store this for a long period, so we can inform the client if the particular redirectUrl has been posted already, or not.
 * That is necessary to provide relevant feedback during an error flow.
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = GeneratedSessionState.TABLE_NAME)
public class GeneratedSessionState {

    public static final String TABLE_NAME = "generated_session_states";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String STATE_ID_COLUMN = "state_id";
    public static final String CREATED_COLUMN = "created";
    public static final String SUBMITTED_COLUMN = "submitted";
    public static final String SUBMITTED_TIME_COLUMN = "submitted_time";
    public static final String USER_SITE_ID_COLUMN = "user_site_id";

    @NonNull
    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @NonNull
    @ClusteringColumn
    @Column(name = STATE_ID_COLUMN)
    private String stateId;

    @NonNull
    @Column(name = CREATED_COLUMN)
    private Date created;

    @NonNull
    @Column(name = SUBMITTED_COLUMN)
    private Boolean submitted;

    @Column(name = SUBMITTED_TIME_COLUMN)
    private Date submittedTime;

    @NonNull
    @Column(name = USER_SITE_ID_COLUMN)
    private UUID userSiteId;

}
