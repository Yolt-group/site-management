package nl.ing.lovebird.sitemanagement.health.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.UserSiteDTOv1;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.health.service.domain.UserHealth;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteService;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final AccountsServiceV1 accountsService;

    private final LegacyUserSiteService legacyUserSiteService;
    private final UserSiteService userSiteService;
    private final SiteService siteService;

    public UserHealth getUserHealth(ClientUserToken clientUserToken) {
        List<AccountGroup> accountGroups = getAccountGroups(clientUserToken);
        List<UserSiteWithAccounts> userSitesWithAccounts = getUserSitesWithAccounts(clientUserToken.getUserIdClaim(), accountGroups);

        List<LovebirdHealthCode> userSitesHealthCodes = userSitesWithAccounts.stream()
                .map(UserSiteWithAccounts::getLovebirdHealthCode)
                .collect(Collectors.toList());

        return new UserHealth(getOverallHealth(userSitesHealthCodes), userSitesWithAccounts, accountGroups);
    }

    /**
     * Return a single {@link LovebirdHealthCode} given a set of {@link LovebirdHealthCode}s.
     * <p/>
     * The {@link UserHealth} has a concept of a single status code which is "calculated" from the {@link LovebirdHealthCode}
     * of all the {@link UserSiteDTOv1}s.
     * <p/>
     * The logic is as follows:
     * - if any of the <code>healthCodes</code> is processing, the overall status is PROCESSING
     * - if any of the <code>healthCodes</code> is UP_TO_DATE, the overall status is UP_TO_DATE
     * - if any of the <code>healthCodes</code> is ERROR, the overall status is ERROR
     * - if none of the <code>healthCodes</code> is trapped in the steps above, the overall status is UNKNOWN
     *
     * @param healthCodes a list of health codes
     * @return the overall health status
     */
    private LovebirdHealthCode getOverallHealth(final Collection<LovebirdHealthCode> healthCodes) {

        if (healthCodes.contains(LovebirdHealthCode.PROCESSING)) {
            return LovebirdHealthCode.PROCESSING;
        }

        if (healthCodes.contains(LovebirdHealthCode.UP_TO_DATE)) {
            return LovebirdHealthCode.UP_TO_DATE;
        }

        if (healthCodes.contains(LovebirdHealthCode.ERROR)) {
            return LovebirdHealthCode.ERROR;
        }

        return LovebirdHealthCode.UNKNOWN;
    }

    private List<AccountGroup> getAccountGroups(ClientUserToken clientUserToken) {
        return accountsService.getAccountGroups(clientUserToken);
    }

    private List<UserSiteWithAccounts> getUserSitesWithAccounts(final UUID userId, final List<AccountGroup> accountGroups) {
        var nonDeletedUserSites = userSiteService.getNonDeletedUserSites(userId).stream()
                .map(legacyUserSiteService::createUserSiteDTO)
                .map(userSiteDTO -> new UserSiteWithAccounts(
                        userSiteDTO.getId(),
                        siteService.getSiteName(userSiteDTO.getSiteId()),
                        userSiteDTO.getStatus(),
                        userSiteDTO.getReason(),
                        userSiteDTO.getStatusTimeoutSeconds(),
                        userSiteDTO.getAction(),
                        userSiteDTO.getMigrationStatus(),
                        userSiteDTO.isNoLongerSupported())
                ).toList();

        if (accountGroups != null) {
            nonDeletedUserSites.forEach(userSite -> userSite.addAccounts(getAccountsForUserSite(userSite.getId(), accountGroups)));
        }

        return nonDeletedUserSites;
    }

    private List<Account> getAccountsForUserSite(final UUID userSiteId, final List<AccountGroup> accountGroups) {
        return accountGroups.stream()
                .flatMap(ag -> ag.getAccounts().stream())
                .filter(account -> account.getUserSiteId().equals(userSiteId))
                .collect(Collectors.toList());
    }
}
