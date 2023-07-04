package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FormTriggerRefreshAndFetchDataDTO {
    private final String externalSiteId;
    private final FormUserSiteDTO formUserSite;
    private final AccessMeansDTO accessMeans;
    private final Instant transactionsFetchStartTime;
    private final UUID providerRequestId;
    private final ClientId clientId;
    private final UserSiteDataFetchInformation userSiteDataFetchInformation;
    private final UUID activityId;
    private final UUID siteId;
}
