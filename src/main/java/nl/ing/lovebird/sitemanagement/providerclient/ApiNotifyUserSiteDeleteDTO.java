package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import org.springframework.lang.Nullable;

@Value
public class ApiNotifyUserSiteDeleteDTO {

    /**
     * The consent ID which is known at the site. This cannot be null, since then it wouldn't make sense to send a notification..
     */
    @NonNull
    String externalConsentId;

    /**
     * The authentication means references that will be used to connect with during this request.
     */
    @NonNull
    AuthenticationMeansReference authenticationMeansReference;

    /**
     * Only known when a PSU triggers the action (instead of e.g. an automated system).
     */
    @Nullable
    String psuIpAddress;

    AccessMeansDTO accessMeansDTO;
}
