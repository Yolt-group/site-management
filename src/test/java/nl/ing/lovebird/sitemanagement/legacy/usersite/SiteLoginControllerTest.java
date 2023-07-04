package nl.ing.lovebird.sitemanagement.legacy.usersite;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providershared.form.*;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.TestRedirectUrlIds;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import nl.ing.lovebird.sitemanagement.usersite.FormStep;
import nl.ing.lovebird.sitemanagement.usersite.RedirectStep;
import nl.ing.lovebird.sitemanagement.usersite.SiteLoginService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.TestUtil.AIS_WITH_REDIRECT_STEPS;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SiteLoginController.class)
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class,
        SiteManagementDebugProperties.class,
})
class SiteLoginControllerTest {

    final UUID siteId = UUID.randomUUID();
    final UUID clientGroupId = UUID.randomUUID();
    final UUID clientId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final Site nonScraperSite = SiteCreatorUtil.createTestSite(siteId.toString(), "site", "NOT_YODLEE", List.of(AccountType.values()), List.of(CountryCode.GB), AIS_WITH_REDIRECT_STEPS);

    final HttpHeaders headers = new HttpHeaders();

    @Autowired
    TestClientTokens testClientTokens;

    @BeforeEach
    void setup() {
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(clientGroupId, clientId, userId);

        headers.put("user-id", Collections.singletonList(userId.toString()));
        headers.put("cbms-profile-id", Collections.singletonList("yolt"));
        headers.put("redirect-url-id", Collections.singletonList(TestRedirectUrlIds.CLIENT_REDIRECT_URL_ID_YOLT_APP.toString()));
        headers.set("client-token", clientUserToken.getSerialized());
    }


    @MockBean
    private SiteLoginService siteLoginService;
    @MockBean
    private UserSiteService userSiteService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetInitiateUserSite() throws Exception {
        final String urlToLoginTo = "some_url";
        RedirectStep redirectStep = new RedirectStep(urlToLoginTo, null, null, UUID.randomUUID());
        when(siteLoginService.createLoginStepForNewUserSite(any(), eq(nonScraperSite.getId()), any(), any()))
                .thenReturn(Pair.of(redirectStep, UUID.randomUUID()));

        this.mockMvc.perform(get("/sites/" + siteId + "/initiate-user-site")
                        .headers(headers)
                )
                .andExpect(status().is(200))
                .andExpect(content().json("{\"redirect\":{\"url\": \"" + urlToLoginTo + "\"}}"))
                .andExpect(content().json("{\"_links\":{\"postLoginStep\":{\"href\":\"/user-sites\"}}}"))
                .andExpect(jsonPath("$.userSiteId").isNotEmpty());

    }

    @Test
    void when_providingHostnameForPsuIpAddress_then_BadRequest() throws Exception {
        this.mockMvc.perform(get("/sites/" + siteId + "/initiate-user-site")
                        .headers(headers)
                        .header("psu-ip-address", "ing.com")
                )
                .andExpect(status().is(400))
                .andExpect(content().json("{\"code\":\"SM1008\",\"message\":\"Method argument not valid (request body validation error)\"}"));
    }

    @Test
    void when_ICallInitiateUserSite_Then_itShouldBeAbleToReturnAProperForm() throws Exception {
        final List<FormComponent> formComponents = createABunchOfFormComponents();

        var stateId = UUID.randomUUID();
        FormStep formStep = new FormStep(
                new Form(formComponents, null, null), null, EncryptionDetailsDTO.NONE, null, stateId);
        when(siteLoginService.createLoginStepForNewUserSite(any(), any(), any(), any())).thenReturn(Pair.of(formStep, UUID.randomUUID()));

        this.mockMvc.perform(get("/sites/" + siteId + "/initiate-user-site")
                        .headers(headers))
                .andExpect(status().is(200))
                .andExpect(content().json("{\"form\" : {\"formComponents\":[{\"id\":\"user1\",\"displayName\":\"user\",\"length\":66,\"maxLength\":6," +
                        "\"optional\":false,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"id\":\"password1\"," +
                        "\"displayName\":\"password\",\"length\":77,\"maxLength\":7,\"optional\":false,\"fieldType\":\"PASSWORD\"," +
                        "\"componentType\":\"FIELD\"},{\"childComponents\":[{\"id\":\"choice-user1\",\"displayName\":\"choice-user\"," +
                        "\"length\":44,\"maxLength\":4,\"optional\":false,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}," +
                        "{\"id\":\"choice-password1\",\"displayName\":\"choice-password\",\"length\":55,\"maxLength\":5," +
                        "\"optional\":false,\"fieldType\":\"PASSWORD\",\"componentType\":\"FIELD\"}," +
                        "{\"childComponents\":[{\"id\":\"multi-user1\",\"displayName\":\"multi-user\",\"length\":11,\"maxLength\":1," +
                        "\"optional\":false,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"},{\"id\":\"multi-password1\"," +
                        "\"displayName\":\"multi-password\",\"length\":22,\"maxLength\":2,\"optional\":false,\"fieldType\":\"PASSWORD\"," +
                        "\"componentType\":\"FIELD\"},{\"id\":\"multi-select1\",\"displayName\":\"multi-select\",\"length\":33," +
                        "\"maxLength\":3,\"optional\":false,\"selectOptionValues\":[{\"displayName\":\"option 1\",\"value\":\"option1\"}," +
                        "{\"displayName\":\"option 2\",\"value\":\"option2\"},{\"displayName\":\"option 3\",\"value\":\"option3\"}]," +
                        "\"fieldType\":\"SELECT\",\"componentType\":\"FIELD\"}],\"displayName\":\"multi\"," +
                        "\"componentType\":\"CONTAINER\",\"containerType\":\"MULTI\"}],\"componentType\":\"CONTAINER\"," +
                        "\"containerType\":\"CHOICE\"},{\"id\":\"select1\",\"displayName\":\"select\",\"length\":88,\"maxLength\":8," +
                        "\"optional\":false,\"selectOptionValues\":[{\"displayName\":\"option 1\",\"value\":\"option1\"}," +
                        "{\"displayName\":\"option 2\",\"value\":\"option2\"}],\"fieldType\":\"SELECT\",\"componentType\":\"FIELD\"}," +
                        "{\"childComponents\":[{\"id\":\"main-multi-user1\",\"displayName\":\"main-multi-user\",\"length\":99," +
                        "\"maxLength\":9,\"optional\":false,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}," +
                        "{\"id\":\"main-multi-user2\",\"displayName\":\"main-multi-user2\",\"length\":1010,\"maxLength\":10," +
                        "\"optional\":false,\"fieldType\":\"TEXT\",\"componentType\":\"FIELD\"}],\"displayName\":\"main-multi\"," +
                        "\"componentType\":\"CONTAINER\",\"containerType\":\"MULTI\"}]}}"))
                .andExpect(content().json("{\"_links\":{\"postLoginStep\":{\"href\":\"/user-sites\"}}}"))
                .andExpect(content().json("{\"form\": {\"stateId\" : \"" + stateId + "\"}}"))
                .andExpect(content().json("{\"form\": {\"encryption\" : {\"type\":\"NONE\"}}}"));

        verify(siteLoginService).createLoginStepForNewUserSite(any(), any(), any(), any());

    }

    private List<FormComponent> createABunchOfFormComponents() {
        final List<FormComponent> formComponents = new ArrayList<>();
        formComponents.add(new TextField("user1", "user", 66, 6, false, null, false));
        formComponents.add(new PasswordField("password1", "password", 77, 7, false, null));

        final ChoiceFormComponent choiceFormComponent = new ChoiceFormComponent();
        choiceFormComponent.addComponent(new TextField("choice-user1", "choice-user", 44, 4, false, null, false));
        choiceFormComponent.addComponent(new PasswordField("choice-password1", "choice-password", 55, 5, false, null));
        formComponents.add(choiceFormComponent);

        final MultiFormComponent multiFormComponent = new MultiFormComponent();
        multiFormComponent.setDisplayName("multi");
        multiFormComponent.addComponent(new TextField("multi-user1", "multi-user", 11, 1, false, null, false));
        multiFormComponent.addComponent(new PasswordField("multi-password1", "multi-password", 22, 2, false, null));
        final SelectField selectField = new SelectField("multi-select1", "multi-select", 33, 3, false, false);
        selectField.addSelectOptionValue(new SelectOptionValue("option1", "option 1"));
        selectField.addSelectOptionValue(new SelectOptionValue("option2", "option 2"));
        selectField.addSelectOptionValue(new SelectOptionValue("option3", "option 3"));
        multiFormComponent.addComponent(selectField);
        choiceFormComponent.addComponent(multiFormComponent);

        final SelectField selectField2 = new SelectField("select1", "select", 88, 8, false, false);
        selectField2.addSelectOptionValue(new SelectOptionValue("option1", "option 1"));
        selectField2.addSelectOptionValue(new SelectOptionValue("option2", "option 2"));
        formComponents.add(selectField2);

        final MultiFormComponent multiFormComponent2 = new MultiFormComponent();
        multiFormComponent2.setDisplayName("main-multi");
        multiFormComponent2.addComponent(new TextField("main-multi-user1", "main-multi-user", 99, 9, false, null, false));
        multiFormComponent2.addComponent(new TextField("main-multi-user2", "main-multi-user2", 1010, 10, false, null, false));
        formComponents.add(multiFormComponent2);
        return formComponents;
    }

    @SpringBootApplication
    public static class LimitedApp {
        @Bean
        public Clock clock() {
            return Clock.systemUTC();
        }
    }
}
