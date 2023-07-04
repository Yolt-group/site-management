package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.providerdomain.AccountType;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserSiteDataFetchInformation {
    String userSiteExternalId;
    UUID userSiteId;
    UUID siteId;
    List<String> userSiteMigratedAccountIds;
    List<AccountType> siteWhiteListedAccountType;
}
