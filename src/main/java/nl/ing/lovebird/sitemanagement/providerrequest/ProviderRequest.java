package nl.ing.lovebird.sitemanagement.providerrequest;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * Context before some asynchronous event is sent to providers.
 * Later on this context is necessary in order to process an asynchronously returned message.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = ProviderRequest.TABLE_NAME)
public class ProviderRequest implements Serializable{

    public static final String TABLE_NAME = "provider_request_v3";

    public static final String ID_COLUMN = "id";
    public static final String ACTIVITY_ID_COLUMN = "activity_id";
    public static final String USER_ID_COLUMN = "user_id";

    @NotNull
    @ClusteringColumn
    @Column(name = ID_COLUMN)
    public UUID id;

    @NotNull
    @Column(name = ACTIVITY_ID_COLUMN)
    public UUID activityId;

    @NotNull
    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @NotNull
    @Column(name = "user_site_id")
    private UUID userSiteId;

    @NotNull
    @Column(name = "user_site_action_type")
    public UserSiteActionType userSiteActionType;

}
