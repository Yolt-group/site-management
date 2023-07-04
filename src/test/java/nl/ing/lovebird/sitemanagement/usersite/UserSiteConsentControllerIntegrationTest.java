package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
class UserSiteConsentControllerIntegrationTest {

    final UUID clientGroupId = UUID.randomUUID();
    final UUID clientId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @Test
    void testGetUserSitesValidation() throws Exception {
        String url = "/user-sites/me";

        String fetchObjectsUrl = url + "?fetchObject=site&fetchObject=yolt";
        HttpEntity<?> httpEntity = createRequestHeader();
        ResponseEntity<String> response = restTemplate.exchange(new URI(fetchObjectsUrl), HttpMethod.GET, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"code\":\"SM1008\",\"message\":\"Method argument not valid (request body validation error)\"}");

        fetchObjectsUrl = url + "?fetchObject=" + StringUtils.repeat("site", 65);
        response = restTemplate.exchange(new URI(fetchObjectsUrl), HttpMethod.GET, httpEntity,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"code\":\"SM1008\",\"message\":\"Method argument not valid (request body validation error)\"}");
    }

    @NotNull
    private HttpEntity<?> createRequestHeader() {
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(clientGroupId, clientId, userId);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("client-token", clientUserToken.getSerialized());
        requestHeaders.add("user-id", userId.toString());
        return new HttpEntity<>(null, requestHeaders);
    }

}
