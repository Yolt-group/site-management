package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.exception.NoRedirectSubjectException;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientRedirectService {
    private final ConsentSessionService userSiteSessionService;
    private final ClientRedirectUrlService clientRedirectUrlService;

    public String getRedirectUrl(final String state) {
        final UUID stateId;
        try {
            stateId = UUID.fromString(state);
        } catch (IllegalStateException e) {
            throw new NoRedirectSubjectException("ConsentSession not found by state " + state + ". Expected a uuid.");
        }
        final Optional<ConsentSession> optionalSession = userSiteSessionService.findByStateId(stateId);

        if (optionalSession.isPresent()) {
            var session = optionalSession.get();
            return clientRedirectUrlService.getRedirectUrlOrThrow(session.getClientId(), session.getRedirectUrlId());
        }

        log.debug("No ConsentSession found that match the given state");
        throw new NoRedirectSubjectException("ConsentSession not found by state.");
    }

}
