package nl.ing.lovebird.sitemanagement.health.controller.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import nl.ing.lovebird.sitemanagement.health.repository.domain.UserSiteWithAccounts;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusCode;
import nl.ing.lovebird.sitemanagement.legacy.usersite.LegacyUserSiteStatusReason;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Schema(name = "UserSiteHealth", description = "The health status for the connection between the user and a site.")
public class UserSiteHealthDTO {

    @NonNull
    @Schema(required = true, allowableValues = "31d984b4-9520-11e6-ae22-56b6b6499611")
    private final UUID id;

    @NonNull
    @Schema(required = true, example = "Yolt Bank, ING", description = "The name of the site this user-site is connected to")
    private final String name;

    @NonNull
    @Schema(required = true, description = "Some statuses can have an optional reason. This only counts for the error states: MFA_FAILED, LOGIN_FAILED, REFRESH_FAILED, EXTERNAL_PARTY_TECHNICAL_ERROR")
    private final LegacyUserSiteStatusCode status;

    @Schema(description = "Failure reason/ details if any")
    private final LegacyUserSiteStatusReason reason;

    @Schema(allowableValues = "60", description = "The time in seconds the current health is valid. Currently only used for MFA_NEEDED")
    private final Long statusTimeoutSeconds;

    @Schema(description = "Some state/reason combinations can mean the user needs to take action. This field shows which action the user should take. Note: this can be a retry after fixing something at their bank (for example in case of a locked account).")
    private final UserSiteNeededAction action;

    @NonNull
    @Schema(required = true, description = "Health of this user-site as LovebirdHealthCode")
    private final LovebirdHealthCode health;

    @NonNull
    @ArraySchema(arraySchema = @Schema(description = "List of accounts", required = true))
    private final List<AccountHealthDTO> accounts;

    @NonNull
    @Schema(required = true, description = "Indicates whether this user can be triggered for migration")
    private final MigrationStatus migrationStatus;

    @Schema(required = true, description = "Indicates if this user-site is no longer supported.")
    private final boolean noLongerSupported;

    public static UserSiteHealthDTO fromUserSite(final UserSiteWithAccounts userSite) {
        final List<AccountHealthDTO> accountDTOs = userSite.getAccounts().stream()
                .map(AccountHealthDTO::fromAccount)
                .collect(Collectors.toList());

        return new UserSiteHealthDTO(
                userSite.getId(), userSite.getSiteName(), userSite.getStatus(), userSite.getReason(),
                userSite.getStatusTimeoutSeconds(), userSite.getAction(),
                userSite.getLovebirdHealthCode(), accountDTOs,
                userSite.getMigrationStatus(), userSite.isNoLongerSupported()
        );
    }
}
