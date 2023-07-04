package nl.ing.lovebird.sitemanagement.health.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "AccountGroup", description = "Information about health of a group of accounts")
public class AccountGroupDTO {

    @NonNull
    @Schema(required = true)
    private final AccountType type;

    @NonNull
    @ArraySchema(arraySchema = @Schema(required = true))
    private final List<AccountHealthDTO> accounts;

    @NonNull
    @Schema(required = true)
    private final LovebirdHealthCode health = LovebirdHealthCode.UP_TO_DATE;

    public static AccountGroupDTO fromAccountGroup(final AccountGroup accountGroup) {
        List<AccountHealthDTO> accountsHealth = accountGroup.getAccounts().stream()
                .map(AccountHealthDTO::fromAccount)
                .sorted(comparing(AccountHealthDTO::getId))
                .collect(Collectors.toList());

        return new AccountGroupDTO(accountGroup.getType(), accountsHealth);
    }
}
