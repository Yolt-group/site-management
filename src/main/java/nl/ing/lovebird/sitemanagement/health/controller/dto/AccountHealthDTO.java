package nl.ing.lovebird.sitemanagement.health.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;

import java.util.Date;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@Schema(name = "AccountHealth", description = "Information about health of a given account")
public class AccountHealthDTO {

    @NonNull
    @Schema(required = true, allowableValues = "31d984b4-9520-11e6-ae22-56b6b6499611")
    private final UUID id;

    @NonNull
    @Deprecated
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    @Schema(required = true, allowableValues = "2016-08-19T10:08:51+0000")
    private final Date lastRefreshed;

    @NonNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    @Schema(required = true, allowableValues = "2016-08-19T10:08:51+0000")
    private final Date updated;

    /**
     * Accounts do not have an explicit status. Back in the day the status was used to record the steps that the pipeline was undertaking to
     * service a refresh so we could inform the user in the yolt app what was going on (as this used to took a long time with Yodlee).
     * <p>
     * As we moved away from the `accounts` pod, these "status transitions" (transactions expected, datascience started) are no longer available.
     * Refresh statuses need to be taken from the `user-site` instead of the `account`, therefore we hard code the value to DATASCIENCE_FINISHED, the end of the pipeline.
     */
    @NonNull
    @Schema(required = true)
    private final String status = "DATASCIENCE_FINISHED";

    @NonNull
    @Schema(required = true, allowableValues = "31d984b4-9520-11e6-ae22-56b6b6499611")
    private final UUID userSiteId;

    @NonNull
    @Schema(required = true)
    private final LovebirdHealthCode health;

    public static AccountHealthDTO fromAccount(final Account account) {
        return new AccountHealthDTO(
                account.getId(),
                account.getLastRefreshed(),
                account.getUpdated(),
                account.getUserSiteId(),
                account.getLovebirdHealthCode());
    }
}
