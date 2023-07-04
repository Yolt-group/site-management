package nl.ing.lovebird.sitemanagement.nonlicensedclients;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.test.TestJwtClaims.createClientClaims;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AuthenticationMeansFactoryTest {

    final UUID clientGroupId = UUID.randomUUID();
    final UUID clientId = UUID.randomUUID();
    final UUID redirectUrlId = UUID.randomUUID();
    final AuthenticationMeansFactory authenticationMeansFactory = new AuthenticationMeansFactory();

    @Test
    void createsAuthenticationMeansForClientId() {
        ClientToken token = new ClientToken("", createClientClaims("junit", clientGroupId, clientId));

        AuthenticationMeansReference authenticationMeansReference = authenticationMeansFactory.createAuthMeans(token, redirectUrlId);
        assertThat(authenticationMeansReference.getClientId()).isEqualTo(clientId);
        assertThat(authenticationMeansReference.getRedirectUrlId()).isEqualTo(redirectUrlId);
        assertThat(authenticationMeansReference.getClientGroupId()).isNull();
    }

}
