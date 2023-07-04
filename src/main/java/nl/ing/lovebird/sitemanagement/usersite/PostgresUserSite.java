package nl.ing.lovebird.sitemanagement.usersite;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.validation.HaveRedirectUrlForUrlProvider;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Entity
@Getter
@ToString
@Table(name = PostgresUserSite.TABLE_NAME)
@Builder(toBuilder = true)
@Setter(value = AccessLevel.PUBLIC)
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@HaveRedirectUrlForUrlProvider
@TypeDefs(@TypeDef(name = "json", typeClass = JsonBinaryType.class))
public class PostgresUserSite implements Serializable {

    public static final String TABLE_NAME = "user_site";
    public static final String USER_ID_COLUMN = "user_id";
    public static final String USER_SITE_ID_COLUMN = "user_site_id";
    public static final String SITE_ID_COLUMN = "site_id";
    public static final String EXTERNAL_ID_COLUMN = "external_id";
    public static final String CONNECTION_STATUS_COLUMN = "connection_status";
    public static final String FAILURE_REASON_COLUMN = "failure_reason";
    public static final String CREATED_COLUMN = "created";
    public static final String UPDATED_COLUMN = "updated";
    public static final String STATUS_TIMEOUT_TIME = "status_timeout_time";
    public static final String LAST_DATA_FETCH_COLUMN = "last_data_fetch";
    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String CLIENT_APPLICATION_ID_COLUMN = "client_application_id";
    public static final String REDIRECT_URL_ID_COLUMN = "redirect_url_id";
    public static final String PROVIDER_COLUMN = "provider";
    public static final String MIGRATION_STATUS_COLUMN = "migration_status";
    public static final String PERSISTED_FORM_STEP_ANSWERS = "persisted_form_step_answers";
    public static final String IS_DELETED_COLUMN = "is_deleted";
    public static final String DELETED_AT_COLUMN = "deleted_at";


    /**
     * Required constructor for wrapped UUID data types (client-id)
     */
    public PostgresUserSite(@NotNull UUID userId, @NotNull UUID userSiteId, @NotNull UUID siteId, @Nullable String externalId, @NotNull ConnectionStatus connectionStatus, @Nullable FailureReason failureReason, @Nullable Instant statusTimeoutTime, @NotNull Instant created, @Nullable Instant updated, @Nullable Instant lastDataFetch, @NotNull ClientId clientId, @NotNull String provider, @Nullable MigrationStatus migrationStatus, @Nullable UUID redirectUrlId, @Nullable Map<String, String> persistedFormStepAnswers, boolean isDeleted, @Nullable Instant deletedAt) {
        this.userId = userId;
        this.userSiteId = userSiteId;
        this.siteId = siteId;
        this.externalId = externalId;
        this.connectionStatus = connectionStatus;
        this.failureReason = failureReason;
        this.statusTimeoutTime = statusTimeoutTime;
        this.created = created;
        this.updated = updated;
        this.lastDataFetch = lastDataFetch;
        this.clientId = clientId.unwrap();
        this.provider = provider;
        this.migrationStatus = migrationStatus;
        this.redirectUrlId = redirectUrlId;
        this.persistedFormStepAnswers = persistedFormStepAnswers;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }

    @NotNull
    @Column(name = USER_ID_COLUMN, nullable = false)
    private UUID userId;

    @Id
    @NotNull
    @Column(name = "id", nullable = false)
    private UUID userSiteId;

    @NotNull
    @Column(name = SITE_ID_COLUMN, nullable = false)
    private UUID siteId;

    @Nullable
    @Column(name = EXTERNAL_ID_COLUMN)
    private String externalId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = CONNECTION_STATUS_COLUMN, nullable = false)
    private ConnectionStatus connectionStatus;

    // Initially nullable until we have migrated over all rows.
    @Enumerated(EnumType.STRING)
    @Column(name = FAILURE_REASON_COLUMN, nullable = false)
    private FailureReason failureReason;

    @Nullable
    @Column(name = STATUS_TIMEOUT_TIME)
    private Instant statusTimeoutTime;

    @NotNull
    @EqualsAndHashCode.Exclude
    @Column(name = CREATED_COLUMN, nullable = false)
    private Instant created;

    @Nullable
    @Column(name = UPDATED_COLUMN)
    @EqualsAndHashCode.Exclude
    private Instant updated;

    /**
     * Point in time for which we have last received data from the bank for this user site.  It is set to the time
     * that the accounts-and-transactions service receives account and transaction data over Kafka from the
     * providers service.
     */
    @Nullable
    @Column(name = LAST_DATA_FETCH_COLUMN)
    private Instant lastDataFetch;

    @NotNull
    @Column(name = CLIENT_ID_COLUMN, nullable = false, columnDefinition = "uuid")
    private UUID clientId;

    public ClientId getClientId() {
        return new ClientId(clientId);
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId.unwrap();
    }

    @NotNull
    @Column(name = PROVIDER_COLUMN, nullable = false)
    public String provider;

    @Nullable
    public MigrationStatus getMigrationStatus() {
        return Optional.ofNullable(migrationStatus)
                .orElse(MigrationStatus.NONE);
    }

    /**
     * @deprecated this field is no longer set, it is only read currently
     */
    @Deprecated
    @Nullable
    @Column(name = MIGRATION_STATUS_COLUMN)
    @Enumerated(EnumType.STRING)
    public MigrationStatus migrationStatus;

    @Nullable
    @Column(name = REDIRECT_URL_ID_COLUMN)
    public UUID redirectUrlId;

    @Nullable
    @Type(type = "json")
    @Column(name = PERSISTED_FORM_STEP_ANSWERS, columnDefinition = "json")
    private Map<String, String> persistedFormStepAnswers;

    @Column(name = IS_DELETED_COLUMN)
    private boolean isDeleted = false;

    @Nullable
    @Column(name = DELETED_AT_COLUMN)
    private Instant deletedAt;


    /**
     * Mark this @{link {@link PostgresUserSite} for deletion by setting the delete flag to true along side a deleted timestamp
     *
     * @param clock the clock to derive the time from
     */
    public void markAsDeleted(final Clock clock) {
        setDeleted(true);
        setDeletedAt(Instant.now(clock));
    }

    public void resetLastDataFetch() {
        setLastDataFetch(null);
    }

    public UserSiteNeededAction determineUserSiteNeededAction() {
        return UserSiteDerivedAttributes.determineNeededAction(this);
    }

    public static class PostgresUserSiteBuilder {
        public PostgresUserSiteBuilder clientId(final ClientId clientId) {
            this.clientId = clientId.unwrap();
            return this;
        }
        public PostgresUserSiteBuilder clientId(final UUID clientId) {
            this.clientId = clientId;
            return this;
        }
    }
}
