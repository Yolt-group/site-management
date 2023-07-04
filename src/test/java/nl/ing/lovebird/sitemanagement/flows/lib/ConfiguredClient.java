package nl.ing.lovebird.sitemanagement.flows.lib;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.http.HttpHeaders;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A client that is registered with Yolt.
 */
@RequiredArgsConstructor
public class ConfiguredClient {

    private final UUID clientId = UUID.randomUUID();
    private final UUID clientGroupId = UUID.randomUUID();
    private final TestClientTokens testClientTokens;
    private ClientToken clientToken;


    public ClientToken getClientToken() {
        if (clientToken == null) {
            clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        }
        return clientToken;
    }

    /**
     * Uniquely identifies the client.
     */
    public ClientId getClientId() {
        return new ClientId(clientId);
    }

    /**
     * Adds (internal) authorization headers to a http request so site-management will recognize the request as
     * coming from a valid client.
     *
     * Adds the {@link ClientToken}.
     */
    public Consumer<HttpHeaders> clientTokenHttpHeaders() {
        return headers -> headers.set("client-token", getClientToken().getSerialized());
    }

    public Consumer<HttpHeaders> clientUserTokenHttpHeaders(UUID userId) {
        return headers -> headers.set("client-token", getClientUserToken(userId).getSerialized());
    }

    public ClientUserToken getClientUserToken(UUID userId) {
        return testClientTokens.createClientUserToken(clientGroupId, clientId, userId);
    }
}
