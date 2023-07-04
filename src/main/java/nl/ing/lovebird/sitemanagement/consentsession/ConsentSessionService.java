package nl.ing.lovebird.sitemanagement.consentsession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.exception.ConsentSessionExpiredException;
import nl.ing.lovebird.sitemanagement.exception.NoSessionException;
import nl.ing.lovebird.sitemanagement.exception.StateAlreadySubmittedException;
import nl.ing.lovebird.sitemanagement.exception.StateOverwrittenException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.*;
import nl.ing.lovebird.uuid.AISState;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsentSessionService {

    private final Clock clock;
    private final GeneratedSessionStateRepository generatedSessionStateRepository;
    private final ObjectMapper objectMapper;
    private final SitesProvider sitesProvider;
    private final ConsentSessionRepository consentSessionRepository;

    // create

    /**
     * Start a session
     *
     * @param stateId           the nonce used in the round-trip between site-management and client, valid for completion of 1 step only in the flow
     * @param redirectUrlId     optional id of the redirectUrl in case of a redirect step
     * @param providerState     keep state for providers, providers is stateless and we ping-pong this state during the flow
     * @param externalConsentId optional external identifier of the consent at the site
     */
    public ConsentSession createConsentSession(
            @NonNull AISState stateId,
            @NonNull ClientId clientId,
            @NonNull UUID userId,
            @NonNull UUID siteId,
            @NonNull Step step,
            UUID redirectUrlId,
            String providerState,
            String externalConsentId
    ) {
        final String serializedStep;
        try {
            serializedStep = objectMapper.writeValueAsString(step);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize step.", e);
        }

        var site = sitesProvider.findByIdOrThrow(siteId);
        var userSiteSession = ConsentSession.builder()
                .operation(ConsentSession.Operation.CREATE_USER_SITE)
                .userSiteId(UUID.randomUUID()) // "reserve" a IUserSite.id
                .userId(userId)
                .stateId(stateId.state())
                .activityId(UUID.randomUUID())
                .siteId(siteId)
                .provider(site.getProvider())
                .redirectUrlId(redirectUrlId)
                .clientId(clientId)
                .providerState(providerState)
                .externalConsentId(externalConsentId)
                .formStep(step instanceof FormStep ? serializedStep : null)
                .redirectUrlStep(step instanceof RedirectStep ? serializedStep : null)
                .created(Instant.now(clock))
                .stepNumber(0)
                .build();
        save(userSiteSession);
        return userSiteSession;
    }

    public ConsentSession createConsentSessionForRenewAccess(
            @NonNull AISState state,
            @NonNull ClientId clientId,
            PostgresUserSite userSite,
            @NonNull Step step,
            UUID redirectUrlId,
            String providerState,
            String externalConsentId
    ) {
        final String serializedStep;
        try {
            serializedStep = objectMapper.writeValueAsString(step);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize step.", e);
        }

        var site = sitesProvider.findByIdOrThrow(userSite.getSiteId());
        var userSiteSession = ConsentSession.builder()
                .operation(ConsentSession.Operation.UPDATE_USER_SITE)
                .userSiteId(userSite.getUserSiteId())
                .userId(userSite.getUserId())
                .stateId(state.state())
                .activityId(UUID.randomUUID())
                .siteId(userSite.getSiteId())
                .provider(site.getProvider())
                .redirectUrlId(redirectUrlId)
                .clientId(clientId)
                .providerState(providerState)
                .externalConsentId(externalConsentId)
                .formStep(step instanceof FormStep ? serializedStep : null)
                .redirectUrlStep(step instanceof RedirectStep ? serializedStep : null)
                .created(Instant.now(clock))
                .stepNumber(0)
                .originalConnectionStatus(userSite.getConnectionStatus())
                .originalFailureReason(userSite.getFailureReason())
                .build();
        save(userSiteSession);
        return userSiteSession;
    }

    /**
     * Note: creating the record in the generated_session_states table that "logs" that we've created a particular
     * stateId for a particular user and usersite has no functional purpose other than to aid our clients with
     * debugging (see {@link CreateOrUpdateUserSiteService})
     * <p>
     * TODO make private again after migration.
     */
    @VisibleForTesting
    void save(@NonNull ConsentSession consentSession) {
        log.info("Creating new user session for site: {}, usersite: {}, stateId: {}, operation: {}", consentSession.getSiteId(), consentSession.getUserSiteId(), consentSession.getStateId(), consentSession.getOperation()); //NOSHERIFF
        generatedSessionStateRepository.store(new GeneratedSessionState(consentSession.getUserId(), consentSession.getStateId().toString(), new Date(), false, consentSession.getUserSiteId()));
        consentSessionRepository.save(consentSession);
    }

    // read

    @Transactional
    public ConsentSession findByStateIdAndRotateStateId(@NonNull UUID userId, @NonNull UUID stateId) {

        ConsentSession consentSession = consentSessionRepository.findByUserIdAndStateId(userId, stateId)
                .orElseThrow(() -> {
                    // Find out why it doesn't exist.
                    List<GeneratedSessionState> generatedSessionStates = generatedSessionStateRepository.get(userId);
                    Optional<GeneratedSessionState> state = generatedSessionStates
                            .stream()
                            .filter(it -> it.getStateId().equals(stateId.toString()))
                            .findFirst();
                    if (state.isEmpty()) {
                        // Not known whatsoever, really wrong input.
                        throw new NoSessionException("State " + stateId + " is unknown. Can't find the related session");
                    }

                    GeneratedSessionState generatedSessionState = state.get();
                    if (Boolean.TRUE.equals(generatedSessionState.getSubmitted())) {
                        // It's already submitted.
                        throw new StateAlreadySubmittedException("State " + stateId + " is already submitted on " + generatedSessionState.getSubmittedTime());
                    }

                    // Now: it was ever present, but not submitted.
                    // That means it's either overwritten, or expired (by ttl)

                    UUID userSiteId = generatedSessionState.getUserSiteId();
                    // Check if there's a more recent consent session that has overridden it:
                    consentSessionRepository.findById(new ConsentSession.ConsentSessionId(userId, userSiteId))
                            .ifPresent(newConsentSession -> {
                                        GeneratedSessionState relatedGeneratedSessionState = generatedSessionStates.stream()
                                                .filter(it -> it.getStateId().equals(newConsentSession.getStateId().toString()))
                                                .findFirst()
                                                .orElseThrow(() -> // Should never happen, but gracefully throw generic 'no session exception'.
                                                        new NoSessionException("Unable to find latest session."));

                                        throw new StateOverwrittenException("state " + stateId + " created at " + generatedSessionState.getCreated() + " is not valid anymore. " +
                                                "A newer session has been created at " + relatedGeneratedSessionState.getCreated());
                                    }
                            );

                    // Otherwise, it's just gone from the table by the ttl.
                    throw new ConsentSessionExpiredException("Session is expired by TTL. Creation " + generatedSessionState.getCreated());
                });

        rotateStateId(consentSession);

        return consentSession;
    }

    public Optional<ConsentSession> findByStateId(@NonNull UUID stateId) {
        return consentSessionRepository.findByStateId(stateId);
    }

    public Optional<ConsentSession> findByUserSiteId(@NonNull UUID userId, @NonNull UUID userSiteId) {
        return consentSessionRepository.findById(new ConsentSession.ConsentSessionId(userId, userSiteId));
    }

    // update


    /**
     * The {@link ConsentSession} keeps track of a 'stateId'.  We only permit a user to submit an id once, so we rotate
     * the id right after a user has submitted the Step corresponding to the stateId.
     * <p>
     * Note: the administration we keep in the generated_session_states table is for debugging purposes *only*.
     */
    private void rotateStateId(@NonNull ConsentSession consentSession) {
        // Mark the state as submitted (for debugging purposes).
        generatedSessionStateRepository.markAsSubmitted(consentSession.getUserId(), consentSession.getStateId().toString());
        // Generate and save a new state id.
        UUID newStateId = AISState.random().state();
        consentSession.setStateId(newStateId);
        consentSessionRepository.save(consentSession);
        // Make a note of the new stateId (for debugging purposes).
        generatedSessionStateRepository.store(new GeneratedSessionState(consentSession.getUserId(), newStateId.toString(), new Date(), false, consentSession.getUserSiteId()));
    }


    public void incrementCompletedSteps(@NonNull ConsentSession consentSession) {
        consentSession.setStepNumber(consentSession.getStepNumber() + 1);
        consentSessionRepository.save(consentSession);
    }

    public void updateWithNewStepAndProviderState(
            @NonNull ConsentSession consentSession,
            @Nullable final String providerState,
            @Nullable final String formStep,
            @Nullable final String redirectStep
    ) {
        final boolean hasEitherFormOrRedirect = StringUtils.isEmpty(formStep) ^ StringUtils.isEmpty(redirectStep);
        if (!hasEitherFormOrRedirect) {
            throw new IllegalArgumentException("either the formstep or redirect step should be populated. Not both!");
        }

        consentSession.setProviderState(providerState);
        consentSession.setFormStep(formStep);
        consentSession.setRedirectUrlStep(redirectStep);
        consentSessionRepository.save(consentSession);
    }

    // delete

    /**
     * Remove sessions for a particular user site.
     */
    public void removeSessionsForUserSite(@NonNull UUID userId, @NonNull UUID userSiteId) {
        ConsentSession.ConsentSessionId consentSessionId = new ConsentSession.ConsentSessionId(userId, userSiteId);
        if (consentSessionRepository.existsById(consentSessionId)) {
            consentSessionRepository.deleteById(consentSessionId);
        }
    }

}
