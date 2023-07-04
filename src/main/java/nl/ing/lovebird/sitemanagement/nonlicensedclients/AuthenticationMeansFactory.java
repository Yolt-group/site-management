package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthenticationMeansFactory {

    /**
     * YTS-client-group is being used by all non-licensed clients that are making use of the YTS license
     */
    private static final UUID YTS_CLIENT_GROUP_ID = UUID.fromString("f767b2f9-5c90-4a4e-b728-9c9c8dadce4f");

    /**
     * Redirect-url-id of YTS-client-group - all non-licensed clients will use the same redirect url of YTS
     */
    private static final UUID YTS_REDIRECT_URL_ID = UUID.fromString("7a900fdd-2048-4359-975a-d2646f2718a8");

    public AuthenticationMeansReference createAuthMeans(ClientToken clientToken, UUID redirectUrlId) {
        if (!clientToken.isPSD2Licensed()) {
            return new AuthenticationMeansReference(null, clientToken.getClientGroupIdClaim(), YTS_REDIRECT_URL_ID);
        }
        return new AuthenticationMeansReference(clientToken.getClientIdClaim(), redirectUrlId);
    }

}
