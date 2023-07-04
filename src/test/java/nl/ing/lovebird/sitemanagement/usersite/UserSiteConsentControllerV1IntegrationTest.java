package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.TestRedirectUrlIds;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.site.SiteService.ID_YOLTBANK_YOLT_PROVIDER;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestContext
public class UserSiteConsentControllerV1IntegrationTest {

    private static final String PSU_IP_ADDRESS_HEADER = "PSU-IP-Address";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    @Test
    void when_psuIpAddress_is_invalid_then_it_rejected() throws Exception {
        UUID userId = UUID.fromString("d834de66-8de3-4425-8a0d-79bb56d7331d"); // don't change this. it has some wiremock mappings attached.

        ClientUserToken clientToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);

        this.mockMvc.perform(post("/v1/users/{userId}/connect?site={siteId}&redirectUrlId={redirectUrlId}", userId, ID_YOLTBANK_YOLT_PROVIDER, TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));

        this.mockMvc.perform(delete("/v1/users/{userId}/user-sites/{userSiteId}", userId, UUID.randomUUID())
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));

        this.mockMvc.perform(post("/v1/users/{userId}/user-sites", userId)
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "http://example.com")
                        .toString())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));

        this.mockMvc.perform(put("/v1/users/{userId}/user-sites/refresh", userId)
                .header("client-token", clientToken)
                .header("redirect-url-id", TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString())
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));

        this.mockMvc.perform(put("/v1/users/{userId}/user-sites/{userSiteId}/refresh", userId, UUID.randomUUID())
                .header("client-token", clientToken)
                .header("redirect-url-id", TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString())
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));

        this.mockMvc.perform(post("/v1/users/{userId}/user-sites/{userSiteId}/renew-access?redirectUrlId={redirectUrlId}", userId, UUID.randomUUID(), TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
                .header(PSU_IP_ADDRESS_HEADER, " ")
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")));
    }

    @Test
    void when_psuIpAddress_is_not_provided_then_a_error() throws Exception {
        UUID userId = UUID.fromString("d834de66-8de3-4425-8a0d-79bb56d7331d"); // don't change this. it has some wiremock mappings attached.

        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(delete("/v1/users/{userId}/user-sites/{userSiteId}", userId, UUID.randomUUID())
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));

        this.mockMvc.perform(post("/v1/users/{userId}/connect?site={siteId}&redirectUrlId={redirectUrlId}", userId, ID_YOLTBANK_YOLT_PROVIDER, TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));

        this.mockMvc.perform(post("/v1/users/{userId}/user-sites", userId)
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "http://example.com")
                        .toString())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));

        this.mockMvc.perform(put("/v1/users/{userId}/user-sites/refresh", userId)
                .header("client-token", clientToken)
                .header("redirect-url-id", TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString())
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));

        this.mockMvc.perform(put("/v1/users/{userId}/user-sites/{userSiteId}/refresh", userId, UUID.randomUUID())
                .header("client-token", clientToken)
                .header("redirect-url-id", TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString())
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));

        this.mockMvc.perform(post("/v1/users/{userId}/user-sites/{userSiteId}/renew-access?redirectUrlId={redirectUrlId}", userId, UUID.randomUUID(), TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP)
                .header("client-token", clientToken)
                .header("user-id", userId.toString())
        )
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message", is("Missing header. Offending http header: PSU-IP-Address")));
    }
}
