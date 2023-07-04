package nl.ing.lovebird.sitemanagement.health.service;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.AccountsAndTransactionsClient;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * Accounts client V1
 */
@RequiredArgsConstructor
@Service
public class AccountsServiceV1 {

    private final Clock clock;
    private final AccountsAndTransactionsClient accountsAndTransactionsClient;

    public List<AccountDTOv1> getAccounts(ClientUserToken clientUserToken) {
        return accountsAndTransactionsClient.getAccounts(clientUserToken);
    }

    /**
     * Return the {@link Account}s grouped by {@link AccountDTOv1#type} wrapped as {@link AccountGroup}
     *
     * @param clientUserToken
     * @return a list of {@link AccountGroup}
     */
    public List<AccountGroup> getAccountGroups(ClientUserToken clientUserToken) {
        var groupedByAccountType = accountsAndTransactionsClient.getAccounts(clientUserToken)
                .stream()
                .collect(groupingBy(accountDTO -> accountDTO.type, mapping(accountDTO -> Account.builder()
                        .id(accountDTO.id)
                        .userSiteId(accountDTO.userSite.userSiteId)
                        .updated(accountDTO.lastDataFetchTime.map(Date::from).orElse(Date.from(Instant.now(clock))))
                        .lastRefreshed(accountDTO.lastDataFetchTime.map(Date::from).orElse(Date.from(Instant.now(clock))))
                        .build(), toList())));

        return groupedByAccountType.entrySet().stream()
                .map(typeAndAccounts -> new AccountGroup(typeAndAccounts.getKey(), typeAndAccounts.getValue()))
                .collect(toList());
    }
}
