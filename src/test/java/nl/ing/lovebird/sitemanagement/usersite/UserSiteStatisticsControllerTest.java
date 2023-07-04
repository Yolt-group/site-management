package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.List.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(UserSiteStatisticsController.class)
@Import({
        ExceptionHandlers.class
})
class UserSiteStatisticsControllerTest {

    @Autowired
    private Clock clock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    @MockBean
    private UserSiteService userSiteService;

    @Test
    void shouldReturnEmpty() throws Exception {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), UUID.randomUUID());

        Mockito.when(userSiteService.getUserSiteStatistics(any()))
                .thenReturn(List.of());

        URI endpoint = UriComponentsBuilder.fromUriString("/internal/clients/self/user-sites/-/statistics")
                .build()
                .toUri();

        mockMvc.perform(get(endpoint)
                .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics", empty()));
    }

    @Test
    void shouldReturnNonEmpty() throws Exception {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), UUID.randomUUID());

        String Id = "7670247e-323e-4275-82f6-87f31119dbd3";

        Mockito.when(userSiteService.getUserSiteStatistics(any()))
                .thenReturn(of(UserSiteService.UserSiteStatistics.builder()
                        .siteId(UUID.fromString(Id))
                        .siteName("ABN AMRO")
                        .nrOfUniqueUsers(1)
                        .nrOfUniqueConnections(31)
                        .connectionStatuses(Map.of(
                                UserSiteService.GeneralizedConnectionStatus.ACTIVE, 1,
                                UserSiteService.GeneralizedConnectionStatus.UNABLE_TO_LOGIN, 2,
                                UserSiteService.GeneralizedConnectionStatus.ERROR, 3,
                                UserSiteService.GeneralizedConnectionStatus.OTHER, 4))
                        .compiledAt(ZonedDateTime.now(clock))
                        .build()));

        URI endpoint = UriComponentsBuilder.fromUriString("/internal/clients/self/user-sites/-/statistics")
                .build()
                .toUri();

        mockMvc.perform(get(endpoint)
                .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics[0].siteId", is(Id)))
                .andExpect(jsonPath("$.statistics[0].siteName", is("ABN AMRO")))
                .andExpect(jsonPath("$.statistics[0].nrOfUniqueUsers", is(1)))
                .andExpect(jsonPath("$.statistics[0].nrOfUniqueConnections", is(31)));
    }

    @SpringBootApplication
    public static class LimitedApp {

        @Bean
        public Clock clock() {
            return Clock.systemUTC();
        }
    }
}