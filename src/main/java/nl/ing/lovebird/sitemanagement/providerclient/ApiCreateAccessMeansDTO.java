package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiCreateAccessMeansDTO {

    @NonNull
    UUID userId;
    @NonNull
    AuthenticationMeansReference authenticationMeansReference;
    @Nullable
    String providerState;
    @Nullable
    FilledInUserSiteFormValues filledInUserSiteFormValues;

    /**
     * Only known when a PSU triggers the action (instead of e.g. an automated system).
     */
    @Nullable
    String psuIpAddress;

    @NonNull
    UUID state;

    /**
     * This is the URL to which the user was redirected *by the bank* after giving their consent.
     *
     * After a user has given their consent, the bank redirects the user to baseClientRedirectUrl with some
     * additional parameters added by the bank, one of which is typically "code", the value of which can be
     * exchanged for a token at the bank (oAuth2).
     *
     * This is **EMPTY** if the user has submitted a form (when {@link #filledInUserSiteFormValues} is filled).
     *
     * Example: "http://www.yolt.com/callback?authorization_code=<some-state-added-by-the-bank/>&state=<some-state-generated-by-Yolt/>"
     */
    @Nullable
    String redirectUrlPostedBackFromSite;

    /**
     * This is the base url registered by a client of Yolt.  This url typically points to
     * a system that is in control of a client.
     *
     * When generating the url to redirect the user **to the bank** to acquire the users consent, this
     * field is typically included as the value for the "redirect_uri" query parameter (oAuth2).
     *
     * Example: "http://www.yolt.com/callback"
     */
    @NonNull
    String baseClientRedirectUrl;
}