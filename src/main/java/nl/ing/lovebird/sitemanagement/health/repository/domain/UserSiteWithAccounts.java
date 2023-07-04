package nl.ing.lovebird.sitemanagement.health.repository.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusCode;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusReason;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode.*;

@Data
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSiteWithAccounts implements LovebirdHealth {
    private final UUID id;
    private final String siteName;
    private final LegacyUserSiteStatusCode status;
    private final LegacyUserSiteStatusReason reason;
    private final Long statusTimeoutSeconds;
    private final UserSiteNeededAction action;
    private final List<Account> accounts = new ArrayList<>();
    private final MigrationStatus migrationStatus;
    private final boolean noLongerSupported;

    /**
     * Return the {@link LovebirdHealthCode} for this user-site.
     * The set of {@link LegacyUserSiteStatusCode} is reduced to {@link LovebirdHealthCode}.
     *
     * @return the {@link LovebirdHealthCode} for this user-site
     */
    public LovebirdHealthCode getLovebirdHealthCode() {
        return switch (status) {
            case INITIAL_PROCESSING, LOGIN_SUCCEEDED -> PROCESSING;
            case LOGIN_FAILED, REFRESH_FAILED -> ERROR;
            case STEP_NEEDED, REFRESH_FINISHED -> UP_TO_DATE;
            case UNKNOWN -> UNKNOWN;
        };
    }

    public void addAccounts(final List<Account> accounts) {
        this.accounts.addAll(accounts);
    }
}

