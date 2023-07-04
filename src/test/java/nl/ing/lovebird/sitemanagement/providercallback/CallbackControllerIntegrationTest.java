package nl.ing.lovebird.sitemanagement.providercallback;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.providercallback.CallbackController.MAX_CALLBACK_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class CallbackControllerIntegrationTest {
    final UUID clientGroupId = UUID.randomUUID();
    final UUID clientId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @Test
    void callbackIsNotAcceptedWhenBodyIsEmpty() {
        HttpEntity<?> httpEntity = createHttpEntity(null);
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/callbacks/BUDGET_INSIGHT", httpEntity, Void.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void callbackIsNotAcceptedWhenBodyIsNull() {
        HttpEntity<?> httpEntity = createHttpEntity(null);
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/callbacks/BUDGET_INSIGHT",
                new HttpEntity<>(null, httpEntity.getHeaders()),
                Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void callbackIsNotAcceptedWhenItExceedsTheLimit() {
        String bigCallbackContent = new String(new char[MAX_CALLBACK_SIZE + 1]);
        HttpEntity<?> httpEntity = createHttpEntity(bigCallbackContent);
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/callbacks/BUDGET_INSIGHT", httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void callbackIsAcceptedWhenItIsOnTheLimit() {
        HttpEntity<?> httpEntity = createHttpEntity("abcdefghijklmnopqrstuvwxyz");
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/callbacks/BUDGET_INSIGHT", httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void callbackOnSubpathIsNotAcceptedWhenItExceedsTheLimit() {
        String bigCallbackContent = new String(new char[MAX_CALLBACK_SIZE + 1]);
        HttpEntity<?> httpEntity = createHttpEntity(bigCallbackContent);
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity("/callbacks/SALTEDGE/notify", httpEntity, Void.class);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpEntity<?> createHttpEntity(Object body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        return createHttpEntity(requestHeaders, body);
    }

    private HttpEntity<?> createHttpEntity(HttpHeaders requestHeaders, Object body) {
        return createHttpEntity(userId, requestHeaders, body);
    }
    private HttpEntity<?> createHttpEntity(UUID userId, HttpHeaders requestHeaders, Object body) {
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(clientGroupId, clientId, userId);
        requestHeaders.add("client-token", clientUserToken.getSerialized());
        requestHeaders.add("user-id", userId.toString());
        return new HttpEntity<>(body, requestHeaders);
    }
}
