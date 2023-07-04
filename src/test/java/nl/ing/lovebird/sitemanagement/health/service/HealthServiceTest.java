package nl.ing.lovebird.sitemanagement.health.service;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteDTO;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteService;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusCode;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusReason;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private AccountsServiceV1 accountsService;
    @Mock
    private LegacyUserSiteService legacyUserSiteService;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private SiteService siteService;

    @InjectMocks
    private HealthService healthService;

    @Test
    void testGetUserHealth() {
        UUID userId = UUID.randomUUID();
        var clientUserToken = mock(ClientUserToken.class);
        when(clientUserToken.getUserIdClaim()).thenReturn(userId);

        var legacyUserSiteOne = getDummyLegacySite();
        var legacyUserSiteTwo = getDummyLegacySite();

        var userSiteOne = mock(PostgresUserSite.class);
        var userSiteTwo = mock(PostgresUserSite.class);

        var accountGroupOne = getDummyAccountGroupOfTypeFor(AccountType.CURRENT_ACCOUNT, legacyUserSiteOne.getId());
        var accountGroupTwo = getDummyAccountGroupOfTypeFor(AccountType.SAVINGS_ACCOUNT, legacyUserSiteTwo.getId());
        var accountGroups = List.of(accountGroupOne, accountGroupTwo);

        var siteOneName = "Site one";
        var siteTwoName = "Site two";

        when(accountsService.getAccountGroups(any())).thenReturn(accountGroups);
        when(userSiteService.getNonDeletedUserSites(userId)).thenReturn(List.of(userSiteOne, userSiteTwo));
        when(legacyUserSiteService.createUserSiteDTO(userSiteOne)).thenReturn(legacyUserSiteOne);
        when(legacyUserSiteService.createUserSiteDTO(userSiteTwo)).thenReturn(legacyUserSiteTwo);
        when(siteService.getSiteName(legacyUserSiteOne.getSiteId())).thenReturn(siteOneName);
        when(siteService.getSiteName(legacyUserSiteTwo.getSiteId())).thenReturn(siteTwoName);

        var userHealth = healthService.getUserHealth(clientUserToken);

        assertThat(userHealth.getLovebirdHealthCode()).isEqualTo(LovebirdHealthCode.UNKNOWN);
        assertThat(userHealth.getAccountGroups()).isEqualTo(accountGroups);
        assertThat(userHealth.getMigrationStatus()).isEqualTo(MigrationStatus.MIGRATING_TO);

        var expectedUserSiteWithAccountsOne = new UserSiteWithAccounts(legacyUserSiteOne.getId(), siteOneName, legacyUserSiteOne.getStatus(),
                legacyUserSiteOne.getReason(), legacyUserSiteOne.getStatusTimeoutSeconds(), legacyUserSiteOne.getAction(),
                legacyUserSiteOne.getMigrationStatus(), false);
        expectedUserSiteWithAccountsOne.addAccounts(accountGroupOne.getAccounts());

        var expectedUserSiteWithAccountsTwo = new UserSiteWithAccounts(legacyUserSiteTwo.getId(), siteTwoName, legacyUserSiteTwo.getStatus(),
                legacyUserSiteTwo.getReason(), legacyUserSiteTwo.getStatusTimeoutSeconds(), legacyUserSiteTwo.getAction(),
                legacyUserSiteTwo.getMigrationStatus(), false);
        expectedUserSiteWithAccountsTwo.addAccounts(accountGroupTwo.getAccounts());

        assertThat(userHealth.getUserSites()).containsExactly(expectedUserSiteWithAccountsOne, expectedUserSiteWithAccountsTwo);
    }

    private LegacyUserSiteDTO getDummyLegacySite() {
        return new LegacyUserSiteDTO(randomUUID(), randomUUID(), LegacyUserSiteStatusCode.UNKNOWN,
                LegacyUserSiteStatusReason.SITE_ERROR, 5L, UserSiteNeededAction.TRIGGER_REFRESH, null, MigrationStatus.MIGRATING_TO,
                new Date(), null, false, null);
    }

    private AccountGroup getDummyAccountGroupOfTypeFor(final AccountType accountType, final UUID userSiteId) {
        return new AccountGroup(accountType, List.of(Account.builder()
                .id(randomUUID())
                .userSiteId(userSiteId)
                .lastRefreshed(new Date())
                .updated(new Date())
                .build()));
    }

}
