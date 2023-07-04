package nl.ing.lovebird.sitemanagement.usersite;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@ToString
@Table(name = PostgresUserSiteAuditLog.TABLE_NAME)
@Builder(toBuilder = true)
@Setter(value = AccessLevel.PUBLIC)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class PostgresUserSiteAuditLog {

    public static final String TABLE_NAME = "user_site_audit_log";

    @Id
    @NotNull
    @Column(name = "user_site_id", nullable = false, updatable = false)
    public UUID userSiteId;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    public UUID userId;

    @NotNull
    @Column(name = "event", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    public PostgresUserSiteAuditLog.AuditEvent event;

    @NotNull
    @Column(name = "metadata", nullable = false, updatable = false)
    public String metadata;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    public enum AuditEvent {
        USER_SITE_DELETED
    }
}
