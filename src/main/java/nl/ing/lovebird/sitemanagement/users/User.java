package nl.ing.lovebird.sitemanagement.users;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.*;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = User.TABLE_NAME)
public class User {

    public static final String TABLE_NAME = "user";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String LAST_LOGIN_COLUMN = "last_login";
    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String STATUS_COLUMN = "status";
    public static final String ONE_OFF_AIS_COLUMN = "one_off_ais";

    @PartitionKey
    @Column(name = USER_ID_COLUMN)
    private UUID userId;

    @Column(name = LAST_LOGIN_COLUMN)
    private Instant lastLogin;

    @Column(name = CLIENT_ID_COLUMN)
    private ClientId clientId;

    @Column(name = STATUS_COLUMN)
    private StatusType status;

    @Getter(AccessLevel.NONE)
    @Column(name = ONE_OFF_AIS_COLUMN)
    private Boolean oneOffAis;

    public boolean isOneOffAis() {
        return Boolean.TRUE.equals(oneOffAis);
    }
}
