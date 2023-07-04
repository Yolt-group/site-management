package nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
@RequiredArgsConstructor
public class AccountDTOv1 {

    @NonNull
    @Schema(required = true, description = "The identifier of the account.", example = "dacba0a6-2305-4359-b942-fea028602a7b")
    public final UUID id;

    @NonNull
    @Schema(required = true)
    public final AccountDTOv1.UserSiteDTOv1 userSite;

    @NonNull
    @Schema(required = true, description = "The account type.", example = "CURRENT_ACCOUNT")
    public final AccountType type;

    @NonNull
    @Schema(description = "The last time account was successfully refreshed.")
    public final Optional<Instant> lastDataFetchTime;

    public UUID getUserSiteId() {
        return userSite.userSiteId;
    }

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    @Schema(name = "The user-site to which this account is linked.")
    public static class UserSiteDTOv1 {

        @NonNull
        @Schema(required = true, description = "The identifier of the user site.", example = "0e62b40a-125a-49d4-a572-4925d51bc4f7")
        public final UUID userSiteId;

        @NonNull
        @Schema(required = true, description = "The identifier of the site.", example = "be44b325-de6d-4993-81a9-2c67e6230253")
        public final UUID siteId;
    }

    public enum AccountType {
        CURRENT_ACCOUNT,
        CREDIT_CARD,
        SAVINGS_ACCOUNT,
        PREPAID_ACCOUNT,
        FOREIGN_CURRENCY,
        PENSION,
        INVESTMENT,
        UNKNOWN
    }
}
