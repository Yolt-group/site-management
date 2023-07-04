package nl.ing.lovebird.sitemanagement.clientconfiguration;

import java.util.UUID;

public class ClientRedirectUrlNotFoundException extends RuntimeException {

    public ClientRedirectUrlNotFoundException(UUID clientId, UUID redirectUrlId) {
        super("Could not find redirect url id '" + redirectUrlId + "' for client '" + clientId + "'");
    }
}
