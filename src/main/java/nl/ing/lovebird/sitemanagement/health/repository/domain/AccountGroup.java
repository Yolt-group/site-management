package nl.ing.lovebird.sitemanagement.health.repository.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NonNull;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountGroup implements LovebirdHealth {

    @NonNull
    private final AccountType type;

    @NonNull
    private final List<Account> accounts;

    private final LovebirdHealthCode lovebirdHealthCode = LovebirdHealthCode.UP_TO_DATE;
}
