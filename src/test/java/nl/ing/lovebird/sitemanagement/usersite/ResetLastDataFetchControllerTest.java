package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Clock;
import java.util.Collections;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {ResetLastDataFetchController.class})
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class
})
class ResetLastDataFetchControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final ClientId CLIENT_ID = ClientIds.YTS_CREDIT_SCORING_APP;

    @MockBean
    private UserSiteMaintenanceService userSiteMaintenanceService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestClientTokens testClientTokens;

    private HttpHeaders headers;


    @BeforeEach
    void setUp() {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), CLIENT_ID.unwrap());
        headers = new HttpHeaders();
        headers.add("client-token", clientToken.getSerialized());
        headers.put("user-id", Collections.singletonList(USER_ID.toString()));
        headers.put("cbms-profile-id", Collections.singletonList("yolt"));
    }

    @Test
    void testResetLastDataFetchForSiteIdWithClientIdHeader() throws Exception {
        UUID siteId = UUID.randomUUID();

        when(userSiteMaintenanceService.resetLastDataFetchForSite(CLIENT_ID, siteId))
                .thenReturn(completedFuture(Collections.emptyList()));

        this.mockMvc.perform(patch(String.format("/sites/%s/reset-last-data-fetch", siteId))
                .headers(headers))
                .andExpect(status().isOk());

        verify(userSiteMaintenanceService).resetLastDataFetchForSite(eq(CLIENT_ID), eq(siteId));
    }

    @Test
    void testResetLastDataFetchForSiteIdWhenClientIdHeaderIsMissing() throws Exception {
        UUID siteId = UUID.randomUUID();
        headers.remove("client-token");

        this.mockMvc.perform(patch(String.format("/sites/%s/reset-last-data-fetch", siteId))
                .headers(headers))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(header().doesNotExist("client-token"))
                .andExpect(status().is(401));
    }

    @SpringBootApplication
    public static class LimitedApp {
        @Bean
        public Clock clock() {
            return Clock.systemUTC();
        }
    }
}
