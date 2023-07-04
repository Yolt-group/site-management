package nl.ing.lovebird.sitemanagement.health.service.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import nl.ing.lovebird.sitemanagement.health.repository.domain.LovebirdHealth;
import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;

import java.util.List;
import java.util.Objects;

@Data
@ToString
public class UserHealth implements LovebirdHealth {

    @NonNull
    private final LovebirdHealthCode lovebirdHealthCode;
    @NonNull
    private final List<UserSiteWithAccounts> userSites;
    @NonNull
    private final List<AccountGroup> accountGroups;

    public MigrationStatus getMigrationStatus() {
        return userSites.stream()
                .map(UserSiteWithAccounts::getMigrationStatus)
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(MigrationStatus.NONE);
    }
}
