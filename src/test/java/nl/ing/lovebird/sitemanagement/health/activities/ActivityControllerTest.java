package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.config.BaseExceptionHandlers;
import nl.ing.lovebird.sitemanagement.SiteManagementApplication;
import nl.ing.lovebird.sitemanagement.health.Activity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= SiteManagementApplication.class)
@WebMvcTest(controllers = ActivityController.class)
@Import({
        BaseExceptionHandlers.class,
})
public class ActivityControllerTest {

    private static final UUID USER_ID = new UUID(0, 0);
    private static final Instant START_TIME = Instant.now().minusSeconds(10);

    @MockBean
    private PersistedActivityService persistedActivityService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    private static final ZonedDateTime epoch = ZonedDateTime.from(Instant.EPOCH.atOffset(ZoneOffset.UTC));

    private Activity activity;
    private ClientUserToken clientToken;

    @BeforeEach
    public void setUp() throws Exception {
        activity = new Activity(UUID.randomUUID(), USER_ID, START_TIME, null, EventType.REFRESH_USER_SITES, new UUID[]{UUID.randomUUID()});
        clientToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), USER_ID);
    }

    @Test
    public void getActivitiesForUser_ifNoRunningOnlyParameterIsPassed_willNotFilterFinishedActivities() throws Exception {
        activity.setEndTime(START_TIME.plusSeconds(1L));
        when(persistedActivityService.getActivitiesForUser(USER_ID)).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/users/{userId}/activities", USER_ID.toString())
                        .header("user-id", USER_ID)
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andDo(print())
                .andExpect(jsonPath("$.activities", hasSize(1)));
    }

    @Test
    public void getActivitiesForUser_ifNoRunningOnlyParameterIsPassed_willNotFilterRunningActivities() throws Exception {
        when(persistedActivityService.getActivitiesForUser(USER_ID)).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/users/{userId}/activities", USER_ID.toString())
                        .header("user-id", USER_ID)
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andDo(print())
                .andExpect(jsonPath("$.activities", hasSize(1)));
    }

    @Test
    public void getActivitiesForUser_ifRunningOnlyParameterIsPassed_willFilterOutFinishedActivities() throws Exception {
        activity.setEndTime(START_TIME.plusSeconds(1L));
        when(persistedActivityService.getActivitiesForUser(USER_ID)).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/users/{userId}/activities", USER_ID.toString())
                        .param("runningOnly", "true")
                        .header("user-id", USER_ID)
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andDo(print())
                .andExpect(jsonPath("$.activities", hasSize(1)));
    }

    @Test
    public void getActivitiesForUser_ifRunningOnlyParameterIsPassed_willNotFilterOutRunningActivities() throws Exception {
        when(persistedActivityService.getActivitiesForUser(USER_ID)).thenReturn(List.of(activity));

        mockMvc.perform(get("/v1/users/{userId}/activities", USER_ID.toString())
                        .param("runningOnly", "true")
                        .header("user-id", USER_ID)
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andDo(print())
                .andExpect(jsonPath("$.activities", hasSize(0)));
    }
}
