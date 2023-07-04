package nl.ing.lovebird.sitemanagement.consentsession;

import lombok.*;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * This entity contains volatile data that we store while the user gives consent.
 */
@Builder
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "consent_session")
@IdClass(ConsentSession.ConsentSessionId.class)
public class ConsentSession {

    @NonNull
    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @NonNull
    @Id
    @Column(name = "user_site_id", columnDefinition = "uuid")
    private UUID userSiteId;

    @Setter
    @NonNull
    @Column(name = "state_id", columnDefinition = "uuid")
    private UUID stateId;

    @NonNull
    @Column(name = "site_id", columnDefinition = "uuid")
    private UUID siteId;

    @NonNull
    @Column(name = "provider")
    private String provider;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "operation")
    private Operation operation;

    /**
     * Can be null for formsteps
     */
    @Column(name = "redirect_url_id", columnDefinition = "uuid")
    private UUID redirectUrlId;

    /**
     * Initially null. Accumulated, (and given back) by providers during subsequent steps.
     */
    @Column(name = "provider_state")
    @Setter
    private String providerState;

    /**
     * Initially null. Will be accumulated and stored if the provider returns this on the getLogin call.
     */
    @Column(name = "external_consent_id")
    private String externalConsentId;

    /**
     * Initially null. Can be populated if a provider returns some next form that needs to be filled in, in order to complete the connection.
     */
    @Setter
    @Column(name = "form_step")
    private String formStep;

    /**
     * Initially null. Can be populated if a provider returns some next redirect step that needs to be followed, in order to complete the connection.
     */
    @Setter
    @Column(name = "redirect_url_step")
    private String redirectUrlStep;

    /**
     * Keeps track of how many steps a user has completed.  A flow can consist of multiple steps (although there usually is one).
     * This value starts at 0 and is incremented everytime a user posts a step and we can successfully complete it.
     */
    @Column(name = "step_number")
    @Setter
    private Integer stepNumber;

    @Column(name = "activity_id", columnDefinition = "uuid")
    @NonNull
    private UUID activityId;

    @NonNull
    @Column(name = "client_id", columnDefinition = "uuid")
    private UUID clientId;

    public ClientId getClientId() {
        return new ClientId(clientId);
    }

    public void setClientId(ClientId clientId) {
        this.clientId = clientId.unwrap();
    }

    @Column(name = "created")
    private Instant created;

    /**
     * Only populated for update operations where after a failure the original state must be put back.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "original_connection_status")
    private ConnectionStatus originalConnectionStatus;

    /**
     * Only populated for update operations where after a failure the original state must be put back.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "original_failure_reason")
    private FailureReason originalFailureReason;

    /**
     * Required constructor for wrapped UUID data types (client-id)
     */
    public ConsentSession(@NonNull UUID userId, @NonNull UUID userSiteId, @NonNull UUID stateId, @NonNull UUID siteId, @NonNull String provider, @NonNull Operation operation, UUID redirectUrlId, String providerState, String externalConsentId, String formStep, String redirectUrlStep, Integer stepNumber, @NonNull UUID activityId, @NonNull ClientId clientId, Instant created, ConnectionStatus originalConnectionStatus, FailureReason originalFailureReason) {
        this.userId = userId;
        this.userSiteId = userSiteId;
        this.stateId = stateId;
        this.siteId = siteId;
        this.provider = provider;
        this.operation = operation;
        this.redirectUrlId = redirectUrlId;
        this.providerState = providerState;
        this.externalConsentId = externalConsentId;
        this.formStep = formStep;
        this.redirectUrlStep = redirectUrlStep;
        this.stepNumber = stepNumber;
        this.activityId = activityId;
        this.clientId = clientId.unwrap();
        this.created = created;
        this.originalConnectionStatus = originalConnectionStatus;
        this.originalFailureReason = originalFailureReason;
    }

    public enum Operation {
        UPDATE_USER_SITE,
        CREATE_USER_SITE;

        public UserSiteActionType toUserActionType() {
            return switch (this) {
                case UPDATE_USER_SITE -> UserSiteActionType.UPDATE_USER_SITE;
                case CREATE_USER_SITE -> UserSiteActionType.CREATE_USER_SITE;
            };
        }
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConsentSessionId implements Serializable {
        UUID userId;
        UUID userSiteId;
    }

    public static class ConsentSessionBuilder {
        public ConsentSession.ConsentSessionBuilder clientId(final ClientId clientId) {
            this.clientId = clientId.unwrap();
            return this;
        }
        public ConsentSession.ConsentSessionBuilder clientId(final UUID clientId) {
            this.clientId = clientId;
            return this;
        }
    }
}
