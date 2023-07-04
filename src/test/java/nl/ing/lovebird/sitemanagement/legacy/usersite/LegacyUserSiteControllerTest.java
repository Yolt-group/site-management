package nl.ing.lovebird.sitemanagement.legacy.usersite;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.NonNull;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.providershared.form.TextField;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.TestRedirectUrlIds;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.site.LoginType;
import nl.ing.lovebird.sitemanagement.site.SiteDTOMapper;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.*;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.Clock.systemUTC;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusCode.*;
import static nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusReason.*;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.AIS_WITH_FORM_STEPS;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.COUNTRY_CODES_GB;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction.*;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.createRandomUserSite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserSiteController.class)
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class,
        SiteManagementDebugProperties.class,
})
class LegacyUserSiteControllerTest {

    private static final UUID REGULAR_USER_ID = UUID.randomUUID();
    private static final User REGULAR_USER = new User(REGULAR_USER_ID, null, new ClientId(UUID.randomUUID()), StatusType.ACTIVE, false);

    private static final UUID ONE_OFF_AIS_USER_ID = UUID.randomUUID();
    private static final User ONE_OFF_AIS_USER = new User(ONE_OFF_AIS_USER_ID, null, new ClientId(UUID.randomUUID()), StatusType.ACTIVE, true);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final UUID randomClientGroupId = UUID.randomUUID();
    private final UUID randomClientId = UUID.randomUUID();
    private final UUID randomUserId = UUID.randomUUID();
    private final UUID randomSiteId = UUID.randomUUID();
    private final UUID randomUserSiteId = UUID.randomUUID();
    private final PostgresUserSite randomUserSite = new PostgresUserSite();

    @MockBean
    private LegacyUserSiteService legacyUserSiteService;
    @MockBean
    private UserService userService;
    @MockBean
    private UserSiteService userSiteService;
    @MockBean
    private CreateOrUpdateUserSiteService createOrUpdateUserSiteService;
    @MockBean
    private SiteDTOMapper siteDTOMapper;
    @MockBean
    private UserSiteDeleteService userSiteDeleteService;
    @MockBean
    private SiteService siteService;
    @MockBean
    private ExternalConsentService externalConsentService;
    @MockBean
    private ConsentSessionService userSiteSessionService;
    @MockBean
    private LastFetchedService lastFetchedService;

    @Autowired
    private MockMvc mockMvc;

    private HttpHeaders headers;

    @MockBean
    private UserSiteRefreshService userSiteRefreshService;

    @Autowired
    private TestClientTokens testClientTokens;

    private ClientUserToken clientUserTokenRandomUser;

    @BeforeEach
    void setUp() {
        randomUserSite.setClientId(ClientId.random());
        clientUserTokenRandomUser = testClientTokens.createClientUserToken(randomClientGroupId, randomClientId, randomUserId);
        headers = new HttpHeaders();
        headers.put("cbms-profile-id", singletonList("yolt-id"));
        headers.put("redirect-url-id", singletonList(TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString()));
    }

    @Test
    void testPostLoginFormInvalidFieldIdSizeTooLarge() throws Exception {

        String content = new JSONObject()
                .put("siteId", randomSiteId)
                .put("filledInFormValues", new JSONArray().put(
                        new JSONObject()
                                .put("fieldId", StringUtils.repeat("a", 1025))
                                .put("value", "fieldXValue")
                ))
                .toString();

        this.mockMvc.perform(post("/user-sites")
                .content(content)
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SM1008")))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")))
                .andReturn();
    }

    @Test
    void testPostLoginFormInvalidTooManyFields() throws Exception {

        JSONArray filledInFormValues = new JSONArray();
        for (int i = 0; i <= 256; i++) {
            filledInFormValues.put(new JSONObject()
                    .put("fieldId", "fieldXid")
                    .put("value", "fieldXValue"));
        }

        String content = new JSONObject()
                .put("siteId", randomSiteId)
                .put("filledInFormValues", filledInFormValues)
                .toString();


        this.mockMvc.perform(post("/user-sites")
                .content(content)
                .headers(headers)
                        .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SM1008")))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")))
                .andReturn();

    }

    @Test
    void testFormValidationError() throws Exception {

        doThrow(new FormValidationException("Some information about the form-error"))
                .when(createOrUpdateUserSiteService).processPostedLogin(any(), any(), any());

        final String body = new JSONObject()
                .put("siteId", randomSiteId)
                .put("stateId", UUID.randomUUID())
                .put("filledInFormValues", new JSONArray()
                        .put(new JSONObject()
                                .put("fieldId", "fieldXid")
                                .put("value", "fieldXValue")))
                .toString();

        this.mockMvc.perform(post("/user-sites")
                .content(body)
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SM043")))
                .andExpect(jsonPath("$.message", is("Invalid form: Some information about the form-error")))
                .andReturn();
    }


    @Test
    void testPostLoginUrl_deprecated() throws Exception {

        when(createOrUpdateUserSiteService.processPostedLogin(any(Login.class), any(), any()))
                .thenReturn(ProcessedStepResult.activity(randomUserSiteId, new UUID(0, 1)));

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED)
                .migrationStatus(MigrationStatus.NONE)
                .action(UserSiteNeededAction.UPDATE_QUESTIONS)
                .reason(LegacyUserSiteStatusReason.SITE_ACTION_NEEDED)
                .build());

        this.mockMvc.perform(post("/user-sites")
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "https://www.yolt.com/")
                        .toString())
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userSiteId", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.status", is(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$.userSite.reason", is(LegacyUserSiteStatusReason.SITE_ACTION_NEEDED.name())))
                .andExpect(jsonPath("$.userSite.action", is(UserSiteNeededAction.UPDATE_QUESTIONS.name())))
                .andExpect(jsonPath("$.userSite.migrationStatus", is(MigrationStatus.NONE.name())))
                .andExpect(content().json(makeFormLinksJson(randomUserSiteId)));

        final ArgumentCaptor<UrlLogin> urlLoginArgumentCaptor = ArgumentCaptor.forClass(UrlLogin.class);

        verify(createOrUpdateUserSiteService).processPostedLogin(urlLoginArgumentCaptor.capture(), any(), any());
        final UrlLogin urlLogin = urlLoginArgumentCaptor.getValue();

        assertThat(urlLogin.getUserId()).isEqualTo(randomUserId);
        assertThat(urlLogin.getRedirectUrl()).isEqualTo("https://www.yolt.com/");
    }

    @Test
    void testPostLoginUrl_Validation() throws Exception {
        final UUID redirectUrlId = TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP;

        this.mockMvc.perform(post("/user-sites")
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "https://www.:-)yolt.com/")
                        .toString())
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SM1008")))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")))
                .andReturn();

        this.mockMvc.perform(post("/user-sites")
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "https://www.yolt:-).com/")
                        .toString())
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .header("redirect-url-id", redirectUrlId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SM1008")))
                .andExpect(jsonPath("$.message", is("Method argument not valid (request body validation error)")))
                .andReturn();
    }

    @Test
    void testGetLoginUrl_ok() throws Exception {

        // Given
        UUID redirectUrlId = TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP;

        String mockedLoginUrl = "mocked-login-url";
        RedirectStep redirectStep = new RedirectStep(mockedLoginUrl, null, null, UUID.randomUUID());
        when(createOrUpdateUserSiteService.createLoginStepToRenewAccess(
                any(), eq(randomUserSiteId), eq(redirectUrlId), any())
        ).thenReturn(redirectStep);

        // When
        this.mockMvc.perform(get("/user-sites/" + randomUserSiteId + "/renew-access")
                .header("user-id", randomUserId)
                .header("client-token", clientUserTokenRandomUser)
                .header("redirect-url-id", redirectUrlId))
                .andExpect(status().isOk())
                // Split in two separate checks to have keep a better overview
                .andExpect(content().json("{\"redirect\":{\"url\":\"" + mockedLoginUrl + "\"}}"))
                .andExpect(jsonPath("$.userSiteId").isNotEmpty())
                .andExpect(content().json("{\"_links\":{\"postLoginStep\":{\"href\":\"/user-sites\"}}}"));
    }

    @Test
    void testGetLoginForm_ok() throws Exception {

        // Given
        UUID redirectUrlId = TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP;
        UUID stateId = UUID.randomUUID();

        final List<FormComponent> formComponents = new ArrayList<>();
        formComponents.add(new TextField("user1", "user", 66, 6, false, null, false));

        when(createOrUpdateUserSiteService.createLoginStepToRenewAccess(
                any(), eq(randomUserSiteId), eq(redirectUrlId), any())
        ).thenReturn(new FormStep(new Form(formComponents, null, null), null, EncryptionDetailsDTO.NONE, null, stateId));

        // When
        this.mockMvc.perform(get("/user-sites/" + randomUserSiteId + "/renew-access")
                .header("user-id", randomUserId)
                .header("redirect-url-id", redirectUrlId)
                .header("client-token", clientUserTokenRandomUser))
                .andExpect(status().isOk())
                // Split in two separate checks to have keep a better overview
                .andExpect(content().json("{\"form\":{\"stateId\":\"" + stateId + "\"}}"))
                .andExpect(content().json("{\"form\":{\"formComponents\":[{\"type\":\"TEXT\",\"id\":\"user1\",\"displayName\":\"user\",\"optional\":false,\"length\":66,\"maxLength\":6,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}]}}"))
                // This needs to change after yolt-app fixed a bug:
                .andExpect(content().json("{\"_links\":{\"postLoginStep\":{\"href\":\"/user-sites\"}}}"))
                .andExpect(content().json("{\"form\":{\"encryption\":{\"type\" : \"NONE\" }}}"));
    }

    @Test
    void testPostLoginUrl_ok() throws Exception {

        final UUID redirectUrlId = UUID.randomUUID();

        when(createOrUpdateUserSiteService.processPostedLogin(
                any(UrlLogin.class),  any(), any())
        ).thenReturn(ProcessedStepResult.activity(randomUserSiteId, new UUID(0, 1)));

        PostgresUserSite userSiteMock = mock(PostgresUserSite.class);
        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSiteMock);
        when(legacyUserSiteService.createUserSiteDTO(userSiteMock)).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED)
                .migrationStatus(MigrationStatus.NONE)
                .action(UserSiteNeededAction.UPDATE_QUESTIONS)
                .reason(LegacyUserSiteStatusReason.SITE_ACTION_NEEDED)
                .build());

        this.mockMvc.perform(post("/user-sites")
                .content(new JSONObject()
                        .put("loginType", "URL")
                        .put("redirectUrl", "https://www.yolt.com/")
                        .toString())
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .header("redirect-url-id", redirectUrlId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userSiteId", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.status", is(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$.userSite.reason", is(LegacyUserSiteStatusReason.SITE_ACTION_NEEDED.name())))
                .andExpect(jsonPath("$.userSite.action", is(UserSiteNeededAction.UPDATE_QUESTIONS.name())))
                .andExpect(jsonPath("$.userSite.migrationStatus", is(MigrationStatus.NONE.name())))
                .andExpect(content().json(makeUrlLinksJson(randomUserSiteId)));

        final ArgumentCaptor<UrlLogin> urlLoginArgumentCaptor = ArgumentCaptor.forClass(UrlLogin.class);

        verify(createOrUpdateUserSiteService).processPostedLogin(urlLoginArgumentCaptor.capture(),  any(), any());
        final UrlLogin urlLogin = urlLoginArgumentCaptor.getValue();

        assertThat(urlLogin.getUserId()).isEqualTo(randomUserId);
        assertThat(urlLogin.getRedirectUrl()).isEqualTo("https://www.yolt.com/");
    }


    @Test
    void testPostLoginForm_ok() throws Exception {

        when(createOrUpdateUserSiteService.processPostedLogin(
                any(FormLogin.class), any(), any()
        )).thenReturn(ProcessedStepResult.activity(randomUserSiteId, new UUID(0, 1)));

        PostgresUserSite userSiteMock = mock(PostgresUserSite.class);
        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSiteMock);
        when(legacyUserSiteService.createUserSiteDTO(userSiteMock)).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_SUCCEEDED)
                .migrationStatus(MigrationStatus.NONE)
                .action(UPDATE_QUESTIONS)
                .reason(SITE_ACTION_NEEDED)
                .build());

        this.mockMvc.perform(post("/user-sites")
                .content(new JSONObject()
                        .put("siteId", randomSiteId)
                        .put("stateId", UUID.randomUUID())
                        .put("filledInFormValues", new JSONArray()
                                .put(new JSONObject()
                                        .put("fieldId", "fieldX")
                                        .put("value", "fieldXValue")
                                )
                                .put(new JSONObject()
                                        .put("fieldId", "fieldY")
                                        .put("value", "fieldYValue")
                                )
                        )
                        .toString())
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userSiteId", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.userSite.status", is(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$.userSite.reason", is(LegacyUserSiteStatusReason.SITE_ACTION_NEEDED.name())))
                .andExpect(jsonPath("$.userSite.action", is(UserSiteNeededAction.UPDATE_QUESTIONS.name())))
                .andExpect(jsonPath("$.userSite.migrationStatus", is(MigrationStatus.NONE.name())))
                .andExpect(content().json(makeFormLinksJson(randomUserSiteId)));

        final ArgumentCaptor<FormLogin> formLoginArgumentCaptor = ArgumentCaptor.forClass(FormLogin.class);

        verify(createOrUpdateUserSiteService).processPostedLogin(
                formLoginArgumentCaptor.capture(), any(), any()
        );
        final FormLogin formLogin = formLoginArgumentCaptor.getValue();


        assertThat(formLogin.getUserId()).isEqualTo(randomUserId);
        final FilledInUserSiteFormValues filledInUserSiteFormValues = formLogin.getFilledInUserSiteFormValues();
        assertThat(filledInUserSiteFormValues.get("fieldX")).isEqualTo("fieldXValue");
        assertThat(filledInUserSiteFormValues.get("fieldY")).isEqualTo("fieldYValue");
    }

    @Test
    void testGetUserSites() throws Exception {
        final UUID siteId1 = UUID.randomUUID();
        final UUID siteId2 = UUID.randomUUID();
        final UUID userSiteId1 = UUID.randomUUID();
        final UUID userSiteId2 = UUID.randomUUID();
        final String externalUserSiteId = "12332123";

        final PostgresUserSite userSite1 = new PostgresUserSite(randomUserId, userSiteId1, siteId1, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, ClientId.random(), "YODLEE", null, null, null, false, null);
        userSite1.setExternalId(externalUserSiteId);
        final PostgresUserSite userSite2 = new PostgresUserSite(randomUserId, userSiteId2, siteId2, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, ClientId.random(), "YOLT_PROVIDER", null, null, null, false, null);
        userSite2.setExternalId(externalUserSiteId);
        final List<PostgresUserSite> userSites = new ArrayList<>();
        userSites.add(userSite1);
        userSites.add(userSite2);

        when(userSiteService.getNonDeletedUserSites(eq(randomUserId))).thenReturn(userSites);

        when(legacyUserSiteService.createUserSiteDTO(userSites.get(0))).thenReturn(LegacyUserSiteDTO.builder()
                .id(userSiteId1)
                .siteId(siteId1)
                .status(LOGIN_SUCCEEDED)
                .build());

        when(legacyUserSiteService.createUserSiteDTO(userSites.get(1))).thenReturn(LegacyUserSiteDTO.builder()
                .id(userSiteId2)
                .siteId(siteId2)
                .status(LOGIN_SUCCEEDED)
                .build());


        this.mockMvc.perform(get("/user-sites/me")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(userSiteId1.toString())))
                .andExpect(jsonPath("$[0].siteId", is(siteId1.toString())))
                .andExpect(jsonPath("$[0].status", is(LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$[0].lastDataFetch", is(nullValue())))
                .andExpect(jsonPath("$[0]._links.site.href", is("/sites/" + siteId1)))
                .andExpect(jsonPath("$[0]._links.refresh.href", is("/user-sites/" + userSiteId1 + "/refresh")))
                .andExpect(jsonPath("$[0]._links.delete.href", is("/user-sites/" + userSiteId1)))
                .andExpect(jsonPath("$[0]._links.renewConsent.href", is("/user-sites/" + userSiteId1 + "/renew-access")))
                .andExpect(jsonPath("$[0]._links.renewAccess.href", is("/user-sites/" + userSiteId1 + "/renew-access")))
                .andExpect(jsonPath("$[0]._links.getNextStep.href", is("/user-sites/" + userSiteId1 + "/step")))
                .andExpect(jsonPath("$[1].id", is(userSiteId2.toString())))
                .andExpect(jsonPath("$[1].siteId", is(siteId2.toString())))
                .andExpect(jsonPath("$[1].status", is(LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$[1].lastDataFetch", is(nullValue())))
                .andExpect(jsonPath("$[1]._links.site.href", is("/sites/" + siteId2)))
                .andExpect(jsonPath("$[1]._links.refresh.href", is("/user-sites/" + userSiteId2 + "/refresh")))
                .andExpect(jsonPath("$[1]._links.delete.href", is("/user-sites/" + userSiteId2)))
                .andExpect(jsonPath("$[1]._links.renewConsent.href", is("/user-sites/" + userSiteId2 + "/renew-access")))
                .andExpect(jsonPath("$[1]._links.getNextStep.href", is("/user-sites/" + userSiteId2 + "/step")))
        ;


        verify(userSiteService).getNonDeletedUserSites(randomUserId);

        verify(legacyUserSiteService, times(2)).createUserSiteDTO(any());
    }


    @Test
    void testGetUserSites_supportsLoginAgainOAuth() throws Exception {

        final String externalUserSiteId = "12332123";

        @NonNull @NotNull Date created = new Date();
        Date deletedAt = Date.from(Instant.now());
        final PostgresUserSite userSite = new PostgresUserSite(randomUserId, randomUserSiteId, randomSiteId, externalUserSiteId, ConnectionStatus.DISCONNECTED, FailureReason.CONSENT_EXPIRED, null != null ? ((Date) null).toInstant() : null, created != null ? created.toInstant() : null, null != null ? ((Date) null).toInstant() : null, null != null ? ((Date) null).toInstant() : null, ClientId.random(), "YODLEE", null, null, null, false, deletedAt != null ? deletedAt.toInstant() : null);
        final List<PostgresUserSite> userSites = new ArrayList<>();
        userSites.add(userSite);

        when(userSiteService.getNonDeletedUserSites(eq(randomUserId))).thenReturn(userSites);

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_FAILED)
                .migrationStatus(MigrationStatus.NONE)
                .action(LOGIN_AGAIN)
                .reason(TOKEN_EXPIRED)
                .build());

        this.mockMvc.perform(get("/user-sites/me")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$[0].siteId", is(randomSiteId.toString())))
                .andExpect(jsonPath("$[0].status", is(LOGIN_FAILED.name())))
                .andExpect(jsonPath("$[0].reason", is(TOKEN_EXPIRED.name())))
                .andExpect(jsonPath("$[0].action", is(LOGIN_AGAIN.name())))
                .andExpect(jsonPath("$[0].lastDataFetch", is(nullValue())))
                .andExpect(jsonPath("$[0]._links.site.href", is("/sites/" + randomSiteId)))
                .andExpect(jsonPath("$[0]._links.refresh.href", is("/user-sites/" + randomUserSiteId + "/refresh")))
                .andExpect(jsonPath("$[0]._links.delete.href", is("/user-sites/" + randomUserSiteId)))
                .andExpect(jsonPath("$[0]._links.getNextStep.href", is("/user-sites/" + randomUserSiteId + "/step")))
        ;

        verify(userSiteService).getNonDeletedUserSites(randomUserId);

        verify(legacyUserSiteService).createUserSiteDTO(any());
    }

    @Test
    void testGetUserSite() throws Exception {

        final String externalUserSiteId = "12332123";

        final PostgresUserSite userSite = createUserSite(randomUserId, randomUserSiteId, randomSiteId, externalUserSiteId);

        String consentExpiresAt = "2019-04-11T11:01:08.126Z";
        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSite);
        when(externalConsentService.findConsentExpiryBy(randomUserId, randomSiteId, randomUserSiteId)).thenReturn(Optional.of(Instant.parse(consentExpiresAt)));


        LocalDateTime randomDateTime = LocalDateTime.of(2018, 6, 6, 15, 15);
        ZonedDateTime randomZonedDateTime = randomDateTime.atZone(ZoneId.of("UTC"));
        final Date randomLastDataFetchDate = Date.from(randomDateTime.atZone(ZoneId.of("UTC")).toInstant());

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_SUCCEEDED)
                .action(UPDATE_QUESTIONS)
                .reason(SITE_ACTION_NEEDED)
                .lastDataFetch(randomLastDataFetchDate)
                .statusTimeoutSeconds(100L)
                .externalConsentExpiresAt(Instant.parse(consentExpiresAt))
                .build());


        this.mockMvc.perform(get(String.format("/user-sites/%s", randomUserSiteId))
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"" + randomUserSiteId + "\",\"siteId\":\"" + randomSiteId + "\",\"status\":\"LOGIN_SUCCEEDED\",\"reason\":\"SITE_ACTION_NEEDED\",\"action\":\"UPDATE_QUESTIONS\"}\"_links\":{\"status\":{\"href\":\"/user-sites/" + randomUserSiteId + "/status\"},\"site\":{\"href\":\"/sites/" + randomSiteId + "\"},\"refresh\":{\"href\":\"/user-sites/" + randomUserSiteId + "/refresh\"},\"delete\":{\"href\":\"/user-sites/" + randomUserSiteId + "\"}}"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.statusTimeoutSeconds").value(greaterThan(50)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastDataFetch").value(is(TIMESTAMP_FORMATTER.format(randomZonedDateTime))))
                .andExpect(MockMvcResultMatchers.jsonPath("$.externalConsentExpiresAt").value(is(consentExpiresAt)));

    }

    @Test
    void testGetUserSite_TimedOut() throws Exception {

        final String externalUserSiteId = "12332123";
        final Date lastDateFetchInPastAsDate = new Date(2018, 3, 1, 7, 45);

        final PostgresUserSite userSite = new PostgresUserSite(randomUserId, randomUserSiteId, randomSiteId, null, ConnectionStatus.DISCONNECTED, FailureReason.ACTION_NEEDED_AT_SITE, null, new Date().toInstant(), null, null, ClientIds.YTS_CREDIT_SCORING_APP, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(externalUserSiteId);
        userSite.setStatusTimeoutTime(Instant.now(systemUTC()).minusSeconds(60));
        userSite.setLastDataFetch(lastDateFetchInPastAsDate.toInstant());

        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSite);

        LocalDateTime randomDateTime = LocalDateTime.of(2019, 8, 22, 14, 30);
        ZonedDateTime randomZonedDateTime = randomDateTime.atZone(ZoneId.of("UTC"));
        final Date randomLastDataFetchDate = Date.from(randomDateTime.atZone(ZoneId.of("UTC")).toInstant());

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_SUCCEEDED)
                .migrationStatus(MigrationStatus.NONE)
                .action(UPDATE_QUESTIONS)
                .reason(SITE_ACTION_NEEDED)
                .lastDataFetch(randomLastDataFetchDate)
                .statusTimeoutSeconds(0L)
                .build());


        this.mockMvc.perform(get(String.format("/user-sites/%s", randomUserSiteId))
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.siteId", is(randomSiteId.toString())))
                .andExpect(jsonPath("$.status", is(LOGIN_SUCCEEDED.name())))
                .andExpect(jsonPath("$.reason", is(SITE_ACTION_NEEDED.name())))
                .andExpect(jsonPath("$.statusTimeoutSeconds", is(0)))
                .andExpect(jsonPath("$.action", is(UPDATE_QUESTIONS.name())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastDataFetch").value(is(TIMESTAMP_FORMATTER.format(randomZonedDateTime))))
                .andExpect(jsonPath("$._links.site.href", is("/sites/" + randomSiteId)))
                .andExpect(jsonPath("$._links.refresh.href", is("/user-sites/" + randomUserSiteId + "/refresh")))
                .andExpect(jsonPath("$._links.delete.href", is("/user-sites/" + randomUserSiteId)))
                .andExpect(jsonPath("$._links.migrationGroup.href", is("")))
                .andExpect(jsonPath("$._links.getNextStep.href", is("/user-sites/" + randomUserSiteId + "/step")))
        ;

    }

    @Test
    void testGetUserSites_StatusFailedWithReason() throws Exception {

        final String externalUserSiteId = "12332123";

        final PostgresUserSite userSite = new PostgresUserSite(randomUserId, randomUserSiteId, randomSiteId, null, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null, new Date().toInstant(), null, null, ClientIds.YTS_CREDIT_SCORING_APP, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(externalUserSiteId);
        final List<PostgresUserSite> userSites = new ArrayList<>();
        userSites.add(userSite);

        when(userSiteService.getNonDeletedUserSites(eq(randomUserId))).thenReturn(userSites);

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_FAILED)
                .migrationStatus(MigrationStatus.NONE)
                .reason(INCORRECT_CREDENTIALS)
                .build());


        this.mockMvc.perform(get("/user-sites/me")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$[0].siteId", is(randomSiteId.toString())))
                .andExpect(jsonPath("$[0].status", is(LOGIN_FAILED.name())))
                .andExpect(jsonPath("$[0].reason", is(INCORRECT_CREDENTIALS.name())))
                .andExpect(jsonPath("$[0].lastDataFetch", is(nullValue())))
                .andExpect(jsonPath("$[0]._links.site.href", is("/sites/" + randomSiteId)))
                .andExpect(jsonPath("$[0]._links.refresh.href", is("/user-sites/" + randomUserSiteId + "/refresh")))
                .andExpect(jsonPath("$[0]._links.delete.href", is("/user-sites/" + randomUserSiteId)))
                .andExpect(jsonPath("$[0]._links.getNextStep.href", is("/user-sites/" + randomUserSiteId + "/step")))
        ;

    }

    @Test
    void testGetUserSites_FetchObjectSite() throws Exception {

        final String externalUserSiteId = "12332123";

        final PostgresUserSite userSite = new PostgresUserSite(randomUserId, randomUserSiteId, randomSiteId, null, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, null, new Date().toInstant(), null, null, ClientIds.YTS_CREDIT_SCORING_APP, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(externalUserSiteId);

        when(userSiteService.getNonDeletedUserSites(eq(randomUserId))).thenReturn(List.of(userSite));

        final String dummySiteName = "OekiepoekieBank";
        when(siteService.getSite(randomSiteId)).thenReturn(createDummySite(randomSiteId, dummySiteName));

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(LOGIN_FAILED)
                .migrationStatus(MigrationStatus.NONE)
                .reason(INCORRECT_CREDENTIALS)
                .build());


        this.mockMvc.perform(get("/user-sites/me")
                .param("fetchObject", "site")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$[0].site.name", is(dummySiteName)))
                .andExpect(jsonPath("$[0].siteId", is(randomSiteId.toString())))
                .andExpect(jsonPath("$[0].status", is(LOGIN_FAILED.name())))
                .andExpect(jsonPath("$[0].reason", is(INCORRECT_CREDENTIALS.name())))
                .andExpect(jsonPath("$[0].statusTimeoutSeconds", is(nullValue())))
                .andExpect(jsonPath("$[0].action", is(nullValue())))
                .andExpect(jsonPath("$[0].lastDataFetch", is(nullValue())))
                .andExpect(jsonPath("$[0]._links.site.href", is("/sites/" + randomSiteId)))
                .andExpect(jsonPath("$[0]._links.refresh.href", is("/user-sites/" + randomUserSiteId + "/refresh")))
                .andExpect(jsonPath("$[0]._links.delete.href", is("/user-sites/" + randomUserSiteId)))
                .andExpect(jsonPath("$[0]._links.getNextStep.href", is("/user-sites/" + randomUserSiteId + "/step")))
        ;

    }


    @Test
    void testGetUserSiteWithFetchObjectParameterSiteValue() throws Exception {

        final String externalUserSiteId = "12332123";
        final PostgresUserSite userSite = new PostgresUserSite(randomUserId, randomUserSiteId, randomSiteId, null, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null, new Date().toInstant(), null, null, ClientIds.YTS_CREDIT_SCORING_APP, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(externalUserSiteId);
        final String siteName = "ING";
        final String groupingBy = "something";
        final List<AccountType> supportedAccountTypes = List.of(AccountType.CURRENT_ACCOUNT);
        final Map<ServiceType, List<LoginRequirement>> usesStepTypes = ImmutableMap.of(ServiceType.AIS, Collections.singletonList(LoginRequirement.FORM));
        final List<CountryCode> availableInCountries = Collections.singletonList(CountryCode.GB);

        var site = SiteCreatorUtil.createTestSite(randomSiteId, siteName, "YODLEE", supportedAccountTypes, availableInCountries, usesStepTypes, groupingBy, null, null, null);

        final Date lastDateFetchInPastAsDate = new Date(0);

        final Map<String, List<String>> usesStepTypesJsonValidateValue = usesStepTypes.entrySet()
                .stream()
                .map(it -> Maps.immutableEntry(it.getKey().name(), it.getValue()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toList())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        userSite.setFailureReason(FailureReason.TECHNICAL_ERROR);
        userSite.setLastDataFetch(lastDateFetchInPastAsDate.toInstant());

        when(userSiteService.getUserSite(eq(randomUserId), eq(randomUserSiteId))).thenReturn(userSite);
        when(siteService.getSite(randomSiteId)).thenReturn(site);


        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .status(UNKNOWN)
                .migrationStatus(MigrationStatus.NONE)
                .action(TRIGGER_REFRESH)
                .reason(GENERIC_ERROR)
                .lastDataFetch(new Date(0L))
                .build());

        this.mockMvc.perform(get("/user-sites/" + randomUserSiteId)
                .param("fetchObject", "site")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(randomUserSiteId.toString())))
                .andExpect(jsonPath("$.siteId", is(randomSiteId.toString())))
                .andExpect(jsonPath("$.reason", is(GENERIC_ERROR.name())))
                .andExpect(jsonPath("$.action", is(TRIGGER_REFRESH.name())))
                .andExpect(jsonPath("$.lastDataFetch", is("1970-01-01T00:00:00.000+0000")))
                .andExpect(jsonPath("$.site.id", is(randomSiteId.toString())))
                .andExpect(jsonPath("$.site.name", is(siteName)))
                .andExpect(jsonPath("$.site.country", is(CountryCode.GB.name())))
                .andExpect(jsonPath("$.site.popular", is(false)))
                .andExpect(jsonPath("$.site.loginType", is(LoginType.FORM.name())))
                .andExpect(jsonPath("$.site.groupingBy", is(groupingBy)))
                .andExpect(jsonPath("$.site.primaryLabel", is("Current accounts")))
                .andExpect(jsonPath("$.site.supportedAccountTypes", is(supportedAccountTypes.stream().map(Enum::name).collect(Collectors.toList()))))
                .andExpect(jsonPath("$.site.usesStepTypes", is(usesStepTypesJsonValidateValue)))
                .andExpect(jsonPath("$.site.availableInCountries", is(availableInCountries.stream().map(Enum::name).collect(Collectors.toList()))))
                .andExpect(jsonPath("$.status", is(UNKNOWN.name())))
                .andExpect(jsonPath("$.statusTimeoutSeconds", is(nullValue())))
                .andExpect(jsonPath("$._links.site.href", is("/sites/" + randomSiteId)))
                .andExpect(jsonPath("$._links.refresh.href", is("/user-sites/" + randomUserSiteId + "/refresh")))
                .andExpect(jsonPath("$._links.delete.href", is("/user-sites/" + randomUserSiteId)))
                .andExpect(jsonPath("$._links.getNextStep.href", is("/user-sites/" + randomUserSiteId + "/step")));
    }

    @Test
    void testRefreshAllUserSites_regularUser_refreshesAllUserSites() throws Exception {
        when(userService.getUserOrThrow(REGULAR_USER_ID)).thenReturn(REGULAR_USER);

        this.mockMvc.perform(put("/user-sites/me/refresh")
                .content("")
                .headers(headers)
                .header("user-id", REGULAR_USER_ID)
                .header("client-token", testClientTokens.createClientUserToken(randomClientGroupId, randomClientId, REGULAR_USER_ID))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("activityId").isEmpty());

        verify(userSiteRefreshService).refreshUserSitesBlocking(any(), eq(false), any(), any(), any(), any());
    }

    @Test
    void testRefreshAllUserSites_oneOffAisUser_returnsBadRequest() throws Exception {
        when(userService.getUserOrThrow(ONE_OFF_AIS_USER_ID)).thenReturn(ONE_OFF_AIS_USER);

        this.mockMvc.perform(put("/user-sites/me/refresh")
                        .content("")
                        .headers(headers)
                        .header("client-token", testClientTokens.createClientUserToken(randomClientGroupId, randomClientId, ONE_OFF_AIS_USER_ID))
                        .header("user-id", ONE_OFF_AIS_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userSiteRefreshService, never()).refreshUserSitesBlocking( any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void testRefreshUserSite_regularUser_refreshesUserSite() throws Exception {
        final var userSite = createRandomUserSite(new ClientId(UUID.randomUUID()), REGULAR_USER_ID, UUID.randomUUID());

        when(userService.getUserOrThrow(REGULAR_USER_ID)).thenReturn(REGULAR_USER);
        when(userSiteService.getUserSite(REGULAR_USER_ID, userSite.getUserSiteId())).thenReturn(userSite);

        this.mockMvc.perform(put("/user-sites/{userSiteId}/refresh", userSite.getUserSiteId())
                        .content("")
                        .headers(headers)
                        .header("client-token", testClientTokens.createClientUserToken(randomClientGroupId, randomClientId, REGULAR_USER_ID))
                        .header("user-id", REGULAR_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("activityId").isEmpty());

        verify(userSiteRefreshService).refreshUserSitesBlocking(eq(singleton(userSite)), eq(false), any(), any(), any(), any());
    }

    @Test
    void testRefreshUserSite_oneOfAisUser_returnsBadRequest() throws Exception {
        when(userService.getUserOrThrow(ONE_OFF_AIS_USER_ID)).thenReturn(ONE_OFF_AIS_USER);

        this.mockMvc.perform(put("/user-sites/{userSiteId}/refresh", UUID.randomUUID())
                        .content("")
                        .headers(headers)
                        .header("client-token", testClientTokens.createClientUserToken(randomClientGroupId, randomClientId, ONE_OFF_AIS_USER_ID))
                        .header("user-id", ONE_OFF_AIS_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userSiteRefreshService, never()).refreshUserSitesBlocking(any(), anyBoolean(), any(), any(), any(), any());
    }

    @Test
    void testDeleteUserSite() throws Exception {
        this.mockMvc.perform(delete(String.format("/user-sites/%s", randomUserSiteId))
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser)
                .header("user-id", randomUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(userSiteDeleteService).deleteExternallyAndMarkForInternalDeletion(eq(randomUserSiteId), eq(null), any(ClientUserToken.class));
    }

    private String makeFormLinksJson(UUID userSiteId) throws JSONException {

        return new JSONObject()
                .put("_links", new JSONObject()
                        .put("userSite", new JSONObject()
                                .put("href", "/user-sites/" + userSiteId)
                        )
                )
                .toString();
    }

    private String makeUrlLinksJson(UUID userSiteId) throws JSONException {
        return new JSONObject()
                .put("_links", new JSONObject()
                        .put("userSite", new JSONObject()
                                .put("href", "/user-sites/" + userSiteId)
                        )
                )
                .toString();
    }

    @Test
    void passingUrlParameterThatCannotBeSerializedGives400Response() throws Exception {

        this.mockMvc.perform(get("/user-sites/null")
                .headers(headers)
                .header("client-token", clientUserTokenRandomUser.getSerialized())
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-id", randomUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserSites_testDateFormatInResponse() throws Exception {
        final Date date = new Date();

        Date deletedAt = Date.from(Instant.now());
        PostgresUserSite userSite = new PostgresUserSite(randomUserId, UUID.randomUUID(), UUID.randomUUID(), "", ConnectionStatus.DISCONNECTED, FailureReason.ACTION_NEEDED_AT_SITE, date != null ? date.toInstant() : null, date != null ? date.toInstant() : null, date != null ? date.toInstant() : null, date != null ? date.toInstant() : null, ClientId.random(), "YOLT_PROVIDER", MigrationStatus.NONE, UUID.randomUUID(), null, false, deletedAt != null ? deletedAt.toInstant() : null);

        when(userSiteService.getNonDeletedUserSites(randomUserId)).thenReturn(List.of(userSite));

        when(legacyUserSiteService.createUserSiteDTO(any())).thenReturn(LegacyUserSiteDTO.builder()
                .id(randomUserSiteId)
                .siteId(randomSiteId)
                .lastDataFetch(date)
                .statusTimeoutSeconds(0L)
                .build());


        mockMvc.perform(get("/user-sites/me")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("user-id", randomUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].lastDataFetch", equalTo(
                        formatDateInGmt("yyyy-MM-dd'T'HH:mm:ss.SSSZ", date))
                ));

        verify(legacyUserSiteService).createUserSiteDTO(any());
    }

    private Site createDummySite(UUID siteId, String siteName) {
        return SiteCreatorUtil.createTestSite(siteId.toString(), siteName, "YODLEE", TestUtil.CURRENT_CREDIT_SAVINGS, COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);
    }

    private static PostgresUserSite createUserSite(UUID userId, UUID userSiteId, UUID siteId, String externalUserSiteId) {
        final Date lastDateFetchInPastAsDate = new Date(2018, 6, 6, 15, 15);
        final PostgresUserSite userSite = new PostgresUserSite(userId, userSiteId, siteId, null, ConnectionStatus.DISCONNECTED, FailureReason.ACTION_NEEDED_AT_SITE, null, new Date().toInstant(), null, null, ClientIds.YTS_CREDIT_SCORING_APP, "YODLEE", null, null, null, false, null);
        userSite.setExternalId(externalUserSiteId);
        userSite.setStatusTimeoutTime(Instant.now(systemUTC()).plusSeconds(60));
        userSite.setLastDataFetch(lastDateFetchInPastAsDate.toInstant());
        return userSite;
    }

    private static String formatDateInGmt(String pattern, Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(date);
    }

    @SpringBootApplication
    public static class LimitedApp {
        @Bean
        public Clock clock() {
            return systemUTC();
        }
    }
}
