package nl.ing.lovebird.sitemanagement.providerclient;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.logging.MDCContextCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

class ProvidersHttpHeadersBuilder {

    private ClientToken clientToken;
    private UUID siteId;

    ProvidersHttpHeadersBuilder siteId(UUID siteId) {
        this.siteId = siteId;
        return this;
    }

    ProvidersHttpHeadersBuilder clientToken(ClientToken clientToken) {
        this.clientToken = clientToken;
        return this;
    }

    HttpHeaders build() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (clientToken != null) {
            headers.set(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        }
        if (siteId != null) {
            headers.set(MDCContextCreator.SITE_ID_MDC_KEY, siteId.toString());
        }

        return headers;
    }
}
