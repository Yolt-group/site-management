package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlNotFoundException;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.sitemanagement.exception.NoRedirectSubjectException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClientRedirectServiceTest {

    @Mock
    private ConsentSessionService userSiteSessionService;

    @Mock
    private ClientRedirectUrlService clientRedirectUrlService;

    @InjectMocks
    private ClientRedirectService systemUnderTest;

    @BeforeEach
    public void setUp() {
        when(userSiteSessionService.findByStateId(any())).thenReturn(Optional.empty());
    }

    @Test
    public void testGetRedirectUrlForConsentSession() {
        var state = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();
        var redirectUrl = "https://redirect/user-site-session/";

        mockConsentSession(state, clientId, redirectUrlId);
        mockRedirectUrl(clientId, redirectUrlId, redirectUrl);

        var result = systemUnderTest.getRedirectUrl(state.toString());

        assertThat(result).isEqualTo(redirectUrl);
    }

    @Test
    public void testGetRedirectUrlWithNoStateMatch() {
        var state = UUID.randomUUID().toString();

        assertThatThrownBy(() -> systemUnderTest.getRedirectUrl(state)).isInstanceOf(NoRedirectSubjectException.class);
    }

    @Test
    public void testGetRedirectUrlForConsentSessionWhenRedirectUrlNotFound() {
        var state = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var redirectUrlId = UUID.randomUUID();

        mockConsentSession(state, clientId, redirectUrlId);
        when(clientRedirectUrlService.getRedirectUrlOrThrow(new ClientId(clientId), redirectUrlId)).thenThrow(
                new ClientRedirectUrlNotFoundException(clientId, redirectUrlId));

        assertThatThrownBy(() -> systemUnderTest.getRedirectUrl(state.toString())).isInstanceOf(ClientRedirectUrlNotFoundException.class);
    }

    private void mockConsentSession(UUID state, UUID clientId, UUID redirectUrlId) {
        var userSiteSession = new ConsentSession(UUID.randomUUID(), UUID.randomUUID(), state, UUID.randomUUID(), "providers", ConsentSession.Operation.CREATE_USER_SITE,
                redirectUrlId, "providerState", null, null, null, 0, UUID.randomUUID(), new ClientId(clientId), Instant.now(), null, null);

        when(userSiteSessionService.findByStateId(state)).thenReturn(Optional.of(userSiteSession));
    }

    private void mockRedirectUrl(UUID clientId, UUID redirectUrlId, String url) {
        when(clientRedirectUrlService.getRedirectUrlOrThrow(new ClientId(clientId), redirectUrlId)).thenReturn(url);
    }
}
