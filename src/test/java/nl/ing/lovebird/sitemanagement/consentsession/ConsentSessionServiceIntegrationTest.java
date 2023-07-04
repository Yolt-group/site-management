package nl.ing.lovebird.sitemanagement.consentsession;

import nl.ing.lovebird.sitemanagement.InstantUntilMillisComparator;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@IntegrationTestContext
class ConsentSessionServiceIntegrationTest {

    @Autowired
    private ConsentSessionService consentSessionService;

    @Test
    void given_aNewConsentSession_then_allReadAndUpdateMethodsShouldSucceed() {
        UUID userId = UUID.randomUUID();
        UUID userSiteId = UUID.randomUUID();
        UUID stateId = UUID.randomUUID();
        ConsentSession userSiteSession = new ConsentSession(
                userId,
                userSiteId,
                stateId,
                UUID.randomUUID(),
                "PROVIDER",
                ConsentSession.Operation.CREATE_USER_SITE,
                UUID.randomUUID(),
                "state",
                null,
                null,
                null,
                0,
                UUID.randomUUID(),
                new ClientId(UUID.randomUUID()),
                Instant.now(),
                null,
                null);
        // This call saves the session in 'the new way'. I.e. in both old and new repo.
        consentSessionService.save(userSiteSession);

        assertThat(consentSessionService.findByUserSiteId(userId, userSiteId)).isPresent();
        assertThat(consentSessionService.findByStateId(stateId)).isPresent();
        assertThat(consentSessionService.findByStateIdAndRotateStateId(userId, stateId))
                .usingRecursiveComparison().ignoringFields("stateId")
                .withComparatorForType(new InstantUntilMillisComparator(), Instant.class) // Nano is not stored..
                .isEqualTo(userSiteSession);

        consentSessionService.incrementCompletedSteps(userSiteSession);
        assertThat(consentSessionService.findByUserSiteId(userId, userSiteId).get().getStepNumber()).isEqualTo(Integer.valueOf(1));

        consentSessionService.updateWithNewStepAndProviderState(userSiteSession, "newstate", "newformstep", null);
        assertThat(consentSessionService.findByUserSiteId(userId, userSiteId).get().getProviderState()).isEqualTo("newstate");

        consentSessionService.removeSessionsForUserSite(userId, userSiteId);
    }

    @Test
    void given_noConsentSession_then_removeSessionsForUserSiteShouldSucceed() {
        assertDoesNotThrow(() -> consentSessionService.removeSessionsForUserSite(UUID.randomUUID(), UUID.randomUUID()));
    }
}