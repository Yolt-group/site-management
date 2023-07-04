package nl.ing.lovebird.sitemanagement.providerclient;

import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvidersHttpHeadersBuilderTest {

    private static final UUID SITE_ID = UUID.randomUUID();
    static final String SITE_ID_HEADER_NAME = "site_id";

    @Test
    void testMakeHeaders_forClientToken() {
        final ClientToken clientToken = mock(ClientToken.class);
        when(clientToken.getClientIdClaim()).thenReturn(UUID.randomUUID());
        when(clientToken.getSerialized()).thenReturn("serialized-client-token");


        var httpHeaders = new ProvidersHttpHeadersBuilder()
                .siteId(SITE_ID)
                .clientToken(clientToken)
                .build();

        assertThat(httpHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(httpHeaders.get(SITE_ID_HEADER_NAME)).containsExactly(SITE_ID.toString());
        assertThat(httpHeaders.get(CLIENT_TOKEN_HEADER_NAME)).containsExactly("serialized-client-token");
    }

    @Test
    void testMakeHeaders_forSiteId() {
        var httpHeaders = new ProvidersHttpHeadersBuilder()
                .siteId(SITE_ID)
                .build();

        assertThat(httpHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(httpHeaders.get(SITE_ID_HEADER_NAME)).containsExactly(SITE_ID.toString());
        assertThat(httpHeaders.get(CLIENT_TOKEN_HEADER_NAME)).isNull();
    }

    @Test
    void testMakeHeaders_withNothing() {
        var httpHeaders = new ProvidersHttpHeadersBuilder()
                .build();

        assertThat(httpHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(httpHeaders.get(SITE_ID_HEADER_NAME)).isNull();
        assertThat(httpHeaders.get(CLIENT_TOKEN_HEADER_NAME)).isNull();
    }
}