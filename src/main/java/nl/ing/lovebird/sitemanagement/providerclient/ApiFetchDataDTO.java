package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

@Value
public class ApiFetchDataDTO {

    @NonNull
    UUID userId;
    @NonNull
    UUID userSiteId;
    @NonNull
    Instant transactionsFetchStartTime;
    @NonNull
    AccessMeansDTO accessMeans;
    @NonNull
    AuthenticationMeansReference authenticationMeansReference;
    @NonNull
    UUID providerRequestId;
    @NonNull
    UUID activityId;
    @NonNull
    UUID siteId;
    /**
     * Only known when a PSU triggers the action (instead of e.g. an automated system).
     */
    @Nullable
    String psuIpAddress;

    UserSiteDataFetchInformation userSiteDataFetchInformation;
}
