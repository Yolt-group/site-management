package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import org.springframework.lang.Nullable;

@Value
public class ApiGetLoginDTO {
    /**
     * Base url for authorization_code callback.
     */
    @NonNull
    String baseClientRedirectUrl;

    /**
     * State parameter value that the ApiDataProvider should include in authorization_code callback.
     */
    @NonNull
    String state;

    @NonNull
    AuthenticationMeansReference authenticationMeansReference;

    /**
     * An optional consent ID which is known at the site. This will always be null if the login URL is for an add bank.
     */
    @Nullable
    String externalConsentId;

    /**
     * Only known when a PSU triggers the action (instead of e.g. an automated system).
     */
    @Nullable
    String psuIpAddress;
}
