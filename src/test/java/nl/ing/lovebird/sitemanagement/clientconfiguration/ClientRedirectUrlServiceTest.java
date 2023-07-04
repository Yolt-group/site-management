package nl.ing.lovebird.sitemanagement.clientconfiguration;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ClientRedirectUrlServiceTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final UUID REDIRECT_URL_ID = UUID.randomUUID();
    private static final String REDIRECT_URL = "redirectUrl";
    public static final String YTS_GROUP_REDIRECT_URL = "https://client-redirect.yts.yolt.io";
    public static final String CONSENT_STARTER_REDIRECT_URL = "https://consent-starter.yts.yolt.io";

    private ClientRedirectUrlRepository clientRedirectUrlRepository = mock(ClientRedirectUrlRepository.class);
    private ClientToken clientToken = mock(ClientToken.class);

    private ClientRedirectUrlService subject = new ClientRedirectUrlService(
            clientRedirectUrlRepository,
            YTS_GROUP_REDIRECT_URL,
            CONSENT_STARTER_REDIRECT_URL,
            systemUTC()
    );

    @Test
    void testDelete_deleteClientRedirectUrl() {
        subject.delete(CLIENT_ID, REDIRECT_URL_ID);

        verify(clientRedirectUrlRepository).delete(CLIENT_ID, REDIRECT_URL_ID);
    }

    @Test
    void testSaveNewClientRedirectUrl_alreadyExists() {
        ArgumentCaptor<ClientRedirectUrl> redirectUrlArgumentCaptor = ArgumentCaptor.forClass(ClientRedirectUrl.class);

        UUID redirectUrlId = UUID.randomUUID();
        when(clientRedirectUrlRepository.get(CLIENT_ID, redirectUrlId)).thenReturn(Optional.of(new ClientRedirectUrl(CLIENT_ID,
                redirectUrlId,
                "https://someredirect",
                Instant.now(systemUTC()))));

        subject.save(CLIENT_ID, null, REDIRECT_URL);

        verify(clientRedirectUrlRepository).saveClientRedirectUrl(redirectUrlArgumentCaptor.capture());

        ClientRedirectUrl clientRedirectUrl = redirectUrlArgumentCaptor.getValue();
        assertThat(clientRedirectUrl.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(clientRedirectUrl.getRedirectUrlId()).isNotNull();
        assertThat(clientRedirectUrl.getUrl()).isEqualTo(REDIRECT_URL);
    }

    @Test
    void testSaveNewRedirectUrl() {
        ArgumentCaptor<ClientRedirectUrl> redirectUrlArgumentCaptor = ArgumentCaptor.forClass(ClientRedirectUrl.class);

        subject.save(CLIENT_ID, null, REDIRECT_URL);

        verify(clientRedirectUrlRepository).saveClientRedirectUrl(redirectUrlArgumentCaptor.capture());

        ClientRedirectUrl clientRedirectUrl = redirectUrlArgumentCaptor.getValue();
        assertThat(clientRedirectUrl.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(clientRedirectUrl.getRedirectUrlId()).isNotNull();
        assertThat(clientRedirectUrl.getUrl()).isEqualTo(REDIRECT_URL);
    }

    @Test
    void changeRedirectBaseUrlOfUrlPostedBackFromSite() {
        // "happy flow"
        assertThat(ClientRedirectUrlService.changeRedirectBaseUrlOfUrlPostedBackFromSite(
                "https://client-redirect.yts.yolt.io",
                "https://www.example.com/oath/callback?param=s"
        )).isEqualTo("https://client-redirect.yts.yolt.io?param=s");

        // no query parameter is added but remains empty (graceful failure)
        assertThat(ClientRedirectUrlService.changeRedirectBaseUrlOfUrlPostedBackFromSite(
                "https://client-redirect.yts.yolt.io",
                "https://www.example.com/oath/callback"
        )).isEqualTo("https://client-redirect.yts.yolt.io");

        // Check that the URI fragment is also copied
        assertThat(ClientRedirectUrlService.changeRedirectBaseUrlOfUrlPostedBackFromSite(
                "https://client-redirect.yts.yolt.io",
                "https://www.example.com/oath/callback?a=b#c=d"
        )).isEqualTo("https://client-redirect.yts.yolt.io?a=b#c=d");

        // Check that the URI fragment is also copied
        assertThat(ClientRedirectUrlService.changeRedirectBaseUrlOfUrlPostedBackFromSite(
                "https://client-redirect.yts.yolt.io/callback",
                "https://client-redirect.yts.yolt.io/callback?a=b#c=d"
        )).isEqualTo("https://client-redirect.yts.yolt.io/callback?a=b#c=d");
    }

    @Test
    void testGetBaseClientRedirectUrlForPaymentWithLicensedClient() {
        var clientRedirectUrl = new ClientRedirectUrl(CLIENT_ID, REDIRECT_URL_ID, "https://client/redirect", Instant.now());

        when(clientRedirectUrlRepository.get(CLIENT_ID, REDIRECT_URL_ID)).thenReturn(Optional.of(clientRedirectUrl));
        when(clientToken.getClientIdClaim()).thenReturn(CLIENT_ID.unwrap());
        when(clientToken.isPSD2Licensed()).thenReturn(true);

        var result = subject.getBaseClientRedirectUrlOrThrow(clientToken, REDIRECT_URL_ID);

        assertThat(result).isEqualTo("https://client/redirect");
    }

    @Test
    void testGetBaseClientRedirectUrlForPaymentWithUnlicensedClient() {
        when(clientToken.isPSD2Licensed()).thenReturn(false);

        var result = subject.getBaseClientRedirectUrlOrThrow(clientToken, REDIRECT_URL_ID);

        assertThat(result).isEqualTo(YTS_GROUP_REDIRECT_URL);
    }

}
