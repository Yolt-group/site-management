package nl.ing.lovebird.sitemanagement.usersite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSiteCleanupService {
    private final Clock clock;
    private final UserSiteService userSiteService;
    private final ConsentSessionService userSiteSessionService;

    public void markWaitingInLoginStepAsTimedOut(long secondsBeforeTimeout) {
        var threshold = Instant.now(clock).minusSeconds(secondsBeforeTimeout);

        userSiteService.getUserSitesWithStepNeeded( /* limit arbitrarily chosen */ 5_000).stream()
                .map(userSite -> Pair.of(userSite, getConsentSession(userSite).orElse(null)))
                .filter(pair -> isTimedOut(pair.getRight(), threshold))
                .forEach(pair -> markAsLoginStepTimedOut(pair.getLeft(), pair.getRight()));
    }

    private boolean isTimedOut(@Nullable ConsentSession userSiteSession, Instant threshold) {
        if (userSiteSession == null) {
            // No user-site-session? Assume it timed-out.
            return true;
        }
        return wasCreatedBeforeThreshold(userSiteSession, threshold);
    }

    private Optional<ConsentSession> getConsentSession(PostgresUserSite userSite) {
        return userSiteSessionService.findByUserSiteId(userSite.getUserId(), userSite.getUserSiteId());
    }

    private boolean wasCreatedBeforeThreshold(ConsentSession userSiteSession, Instant threshold) {
        return ofNullable(userSiteSession.getCreated())
                .map(created -> created.isBefore(threshold))
                .orElse(false); // No created timestamp? Assume it is within the time-window.
    }

    private void markAsLoginStepTimedOut(PostgresUserSite userSite, @Nullable ConsentSession userSiteSession) {
        final ConnectionStatus connectionStatus;
        final FailureReason failureReason;

        if (userSiteSession != null && userSiteSession.getOriginalConnectionStatus() != null) {
            connectionStatus = userSiteSession.getOriginalConnectionStatus();
            failureReason = userSiteSession.getOriginalFailureReason();
            log.info("markAsLoginStepTimedOut: restoring usersite={} to {} with {}", userSite.getUserSiteId(), connectionStatus, failureReason);
        } else {
            connectionStatus = ConnectionStatus.DISCONNECTED;
            failureReason = FailureReason.AUTHENTICATION_FAILED;
            log.info("markAsLoginStepTimedOut: updating usersite={} to DISCONNECTED with AUTHENTICATION_FAILED", userSite.getUserSiteId());
        }

        userSiteService.updateUserSiteStatus(userSite, connectionStatus, failureReason, null);
        // Remove session - we don't need it anymore.
        // Without an existing session, a new session can be started.
        userSiteSessionService.removeSessionsForUserSite(userSite.getUserId(), userSite.getUserSiteId());
    }
}
