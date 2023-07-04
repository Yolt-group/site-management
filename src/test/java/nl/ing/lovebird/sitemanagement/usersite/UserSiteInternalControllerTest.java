package nl.ing.lovebird.sitemanagement.usersite;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.logging.test.CaptureLogEvents;
import nl.ing.lovebird.logging.test.LogEvents;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;
import nl.ing.lovebird.sitemanagement.lib.TestRedirectUrlIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteDTOMapper;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.apache.commons.lang3.RandomStringUtils;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.time.Clock.systemUTC;
import static java.util.Collections.singletonList;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.YOLT_APP_CLIENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserSiteInternalController.class)
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class,
        SiteManagementDebugProperties.class,
})
@CaptureLogEvents
class UserSiteInternalControllerTest {

    private final UUID randomClientGroupId = UUID.randomUUID();
    private final ClientId randomClientId = ClientId.random();
    private final UUID randomUserId = UUID.randomUUID();
    private final PostgresUserSite randomUserSite = new PostgresUserSite();
    private final UUID randomUserSiteId = UUID.randomUUID();
    @MockBean
    private UserSiteService userSiteService;
    @MockBean
    private CreateOrUpdateUserSiteService createOrUpdateUserSiteService;
    @MockBean
    private SiteDTOMapper siteDTOMapper;
    @MockBean
    private UserSiteDeleteService userSiteDeleteService;
    @MockBean
    private ExternalConsentService externalConsentService;
    @MockBean
    private ConsentSessionService userSiteSessionService;
    @MockBean
    private LastFetchedService lastFetchedService;
    @MockBean
    private WebClient.Builder webClientBuilder;
    @MockBean
    private UserSiteRefreshService userSiteRefreshService;
    @MockBean
    private ClientTokenRequesterService clientTokenRequesterService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestClientTokens testClientTokens;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        randomUserSite.setClientId(randomClientId);
        ClientUserToken token = testClientTokens.createClientUserToken(randomClientGroupId, randomClientId.unwrap(), randomUserId);

        headers = new HttpHeaders();
        headers.put("cbms-profile-id", singletonList("yolt-id"));
        headers.put("redirect-url-id", singletonList(TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString()));
        headers.put("client-token", singletonList(token.getSerialized()));
    }

    @Test
    void testGetExternalId() throws Exception {

        final String expectedExternalId = "3456789";
        final PostgresUserSite userSite = new PostgresUserSite();
        userSite.setExternalId(expectedExternalId);
        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSite);


        this.mockMvc.perform(get("/user-sites/" + randomUserSiteId + "/external")
                        .headers(headers)
                        .header("user-id", randomUserId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(new ExternalUserSiteIdDTO(expectedExternalId))));

        verify(userSiteService).getUserSite(randomUserId, randomUserSiteId);
    }

    @Test
    void testPutExternalId() throws Exception {

        final String externalUserSiteId = RandomStringUtils.random(36, true, true);
        String jsonBody = objectMapper.writeValueAsString(new ExternalUserSiteIdDTO(externalUserSiteId));

        //PUT request being tested
        this.mockMvc.perform(put("/user-sites/" + randomUserSiteId + "/external")
                        .headers(headers)
                        .header("user-id", randomUserId)
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userSiteService).updateExternalId(eq(randomUserId), eq(randomUserSiteId), eq(externalUserSiteId));
    }


    @Test
    void testPutExternalId_Invalid_values() {

        Stream.of("", null).forEach(
                invalidExternalId -> {
                    try {
                        this.mockMvc.perform(put("/user-sites/" + UUID.randomUUID() + "/external")
                                        .headers(headers)
                                        .header("user-id", UUID.randomUUID())
                                        .content(objectMapper.writeValueAsString(new ExternalUserSiteIdDTO(invalidExternalId)))
                                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                                .andExpect(status().isBadRequest());
                    } catch (Exception e) {
                        fail();
                    }
                });
    }


    @Test
    void testDeletedExternalId(LogEvents events) throws Exception {

        final String expectedExternalId = RandomStringUtils.random(20, true, true);
        final PostgresUserSite userSite = new PostgresUserSite();
        userSite.setExternalId(expectedExternalId);
        final String errorMessage = String.format("Whatever error message with the User Site Id %s", randomUserSiteId);
        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenThrow(new UserSiteNotFoundException(errorMessage));

        this.mockMvc.perform(get("/user-sites/" + randomUserSiteId + "/external")
                        .headers(headers)
                        .header("user-id", randomUserId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());

        assertThat(events.stream(Level.INFO)
                .anyMatch(iLoggingEvent -> iLoggingEvent.getFormattedMessage().endsWith(errorMessage)))
                .withFailMessage("Expected INFO log event not captured")
                .isTrue();
    }

    @Test
    void testDeleteUserSiteInternal() throws Exception {

        this.mockMvc.perform(delete(String.format("/user-sites/%s/%s", randomUserId, randomUserSiteId))
                        .headers(headers)
                        .header("user-id", randomUserId)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is(200))
                .andExpect(content().string(""));

        verify(userSiteDeleteService).deleteUserSite(randomUserId, randomUserSiteId, null);
    }

    @Test
    void testResetLastDataFetch() throws Exception {

        this.mockMvc.perform(patch(String.format("/user-sites/%s/%s/reset-last-data-fetch", randomUserId, randomUserSiteId))
                        .content("")
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is(200))
                .andExpect(content().string(""));

        verify(userSiteService).resetLastDataFetch(randomUserId, randomUserSiteId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"assistance-portal-yts", "dev-portal"})
    void givenValidClientToken_whenGetUserId_thenReturnUserId(String isfClaim) throws Exception {
        Consumer<JwtClaims> mutator = claims -> claims.setClaim("isf", isfClaim);
        ClientToken token = testClientTokens.createClientToken(randomClientGroupId, YOLT_APP_CLIENT_ID.unwrap(), mutator);

        var userSite = new PostgresUserSite();
        userSite.setUserId(randomUserId);
        when(userSiteService.getUserSiteByClientId(any(ClientId.class), eq(randomUserSiteId))).thenReturn(userSite);

        var userIdDTO = new UserIdDTO(randomUserId);
        this.mockMvc.perform(get(String.format("/user-sites/%s/user", randomUserSiteId))
                        .header("client-token", token.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(userIdDTO)));

        verify(userSiteService).getUserSiteByClientId(any(ClientId.class), eq(randomUserSiteId));
    }

    @Test
    void givenInvalidClientToken_whenGetUserId_thenForbiddenErrorResponseIsReturned() throws Exception {
        Consumer<JwtClaims> mutator = claims -> claims.setClaim("isf", "some-other-portal");
        ClientToken token = testClientTokens.createClientToken(randomClientGroupId, YOLT_APP_CLIENT_ID.unwrap(), mutator);

        var userSite = new PostgresUserSite();
        userSite.setUserId(randomUserId);
        when(userSiteService.getUserSiteByClientId(any(ClientId.class), eq(randomUserSiteId))).thenReturn(userSite);

        var errorResponse = "{\"code\":\"SM9002\",\"message\":\"Token requester for client-token is unauthorized.\"}";
        this.mockMvc.perform(get(String.format("/user-sites/%s/user", randomUserSiteId))
                        .header("client-token", token.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden())
                .andExpect(content().string(errorResponse));

        verify(userSiteService, never()).getUserSiteByClientId(any(ClientId.class), eq(randomUserSiteId));
    }

    @SpringBootApplication
    public static class LimitedApp {
        @Bean
        public Clock clock() {
            return systemUTC();
        }
    }
}
