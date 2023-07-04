package nl.ing.lovebird.sitemanagement.usersitedelete;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Used for audit purposes to prove that user was deleted at Yolt, should NOT be removed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = UserSiteDelete.TABLE_NAME)
public class UserSiteDelete {
    public static final String TABLE_NAME = "user_site_delete";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String USER_SITE_ID_COLUMN = "user_site_id";
    public static final String DELETED_COLUMN = "deleted";
    public static final String EXTERNAL_USER_SITE_ID_COLUMN = "external_user_site_id";

    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @PartitionKey(1)
    @Column(name = USER_SITE_ID_COLUMN)
    private UUID userSiteId;

    @Column(name = EXTERNAL_USER_SITE_ID_COLUMN)
    private String externalUserSiteId;

    @Column(name = DELETED_COLUMN)
    private Date deleted;
}
