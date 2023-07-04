package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import org.springframework.lang.Nullable;

import java.time.Instant;

@Value
public class RefreshAccessMeansDTO {

    @NonNull
    AccessMeansDTO accessMeansDTO;
    @NonNull
    AuthenticationMeansReference authenticationMeansReference;

    /**
     * Only known when a PSU triggers the action (instead of e.g. an automated system).
     */
    @Nullable
    String psuIpAddress;

    @Nullable
    Instant consentCreationTime;

}
