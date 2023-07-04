package nl.ing.lovebird.sitemanagement.usersite;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@ToString
@Table(name = PostgresUserSiteLock.TABLE_NAME)
@Builder(toBuilder = true)
@Setter(value = AccessLevel.PUBLIC)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class PostgresUserSiteLock {

    public static final String TABLE_NAME = "user_site_lock";

    @Id
    @NotNull
    @Column(name = "user_site_id", nullable = false)
    public UUID userSiteId;

    @NotNull
    @Column(name = "activity_id", nullable = false)
    public UUID activityId;

    @Nullable
    @Column(name = "locked_at")
    public Instant lockedAt;
}
