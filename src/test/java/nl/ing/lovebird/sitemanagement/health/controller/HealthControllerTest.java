package nl.ing.lovebird.sitemanagement.health.controller;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.errorhandling.config.BaseExceptionHandlers;
import nl.ing.lovebird.sitemanagement.SiteManagementApplication;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.health.service.HealthService;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.health.service.domain.UserHealth;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusCode;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusReason;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = SiteManagementApplication.class)
@WebMvcTest(controllers = HealthController.class)
@Import({
        BaseExceptionHandlers.class
})
class HealthControllerTest {
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @MockBean
    private HealthService healthService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    ClientUserToken clientUserToken;

    @BeforeEach
    public void setup() {
        final List<AccountGroup> accountGroups = new ArrayList<>();
        final ArrayList<Account> accounts = new ArrayList<>();

        final Date lastRefreshed = Date.from(LocalDateTime.of(2010, 6, 6, 0, 0).toInstant(ZoneOffset.UTC));
        final Date updated = new Date(0);
        final Account account = new Account(ACCOUNT_ID, lastRefreshed, updated, USER_SITE_ID);
        accounts.add(account);
        final AccountGroup accountGroup = new AccountGroup(AccountType.CREDIT_CARD, accounts);
        accountGroups.add(accountGroup);
        final List<UserSiteWithAccounts> userSites = new ArrayList<>();

        final UserSiteWithAccounts userSite = new UserSiteWithAccounts(USER_SITE_ID, "Some Bank Name", LegacyUserSiteStatusCode.REFRESH_FINISHED,
                LegacyUserSiteStatusReason.MULTIPLE_LOGINS, 432L, UserSiteNeededAction.UPDATE_QUESTIONS, MigrationStatus.NONE, false);

        userSite.addAccounts(accounts);
        userSites.add(userSite);
        final UserHealth userHealth = new UserHealth(LovebirdHealthCode.UP_TO_DATE, userSites, accountGroups);

        clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), USER_ID);
        when(healthService.getUserHealth(clientUserToken)).thenReturn(userHealth);
    }

    @Test
    public void testGetUserHealthMe() throws Exception {

        mockMvc.perform(get("/user-health/me")
                        .header("user-id", USER_ID.toString())
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized()))
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        //@formatter:off
                        "\"health\":\"UP_TO_DATE\"," +
                        "\"migrationStatus\":\"NONE\"," +
                        "\"accountGroups\":[{" +
                        "   \"type\":\"CREDIT_CARD\"," +
                        "   \"accounts\":[{\"id\":\"" + ACCOUNT_ID + "\", \"status\":\"DATASCIENCE_FINISHED\",\"userSiteId\":\"" + USER_SITE_ID + "\",\"health\":\"UP_TO_DATE\"}]," +
                        "   \"health\":\"UP_TO_DATE\"}]," +
                        "\"userSites\":[{" +
                        "   \"id\":\"" + USER_SITE_ID + "\",\"name\":\"Some Bank Name\"," +
                        "   \"migrationStatus\":\"NONE\",\"status\":\"REFRESH_FINISHED\",\"reason\":\"MULTIPLE_LOGINS\",\"statusTimeoutSeconds\":432,\"action\":\"UPDATE_QUESTIONS\",\"health\":\"UP_TO_DATE\"," +
                        "   \"accounts\":[{\"id\":\"" + ACCOUNT_ID + "\",\"status\":\"DATASCIENCE_FINISHED\",\"userSiteId\":\"" + USER_SITE_ID + "\",\"health\":\"UP_TO_DATE\"}]" +
                        "}]" +
                        "}"))
                //@formatter:on
                .andDo(print());

    }

    @Test
    public void testGetUserHealth_UnknownException() throws Exception {
        doThrow(RuntimeException.class).when(healthService).getUserHealth(any());

        mockMvc.perform(get("/user-health/me")
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                        .header("user-id", USER_ID.toString()))
                .andExpect(status().is(500))
                .andExpect(content().json("{\"code\":\"SM1000\",\"message\":\"Server error\"}"))
                .andDo(print());

    }

}
