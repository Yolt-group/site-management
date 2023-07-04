package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import nl.ing.lovebird.providerdomain.AccountType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Value
@Schema(name = "UserSite", description = "The connection between the user and a site.")
public class UserSiteDTO {

    @NonNull
    @Schema(required = true)
    UUID id;

    @NonNull
    @Schema(required = true, description = "The connection status. This indicates whether we can fetch new data for this user site.", example = "CONNECTED")
    ConnectionStatus connectionStatus;

    @Nullable
    @Schema(description = "If the last refresh fails, this property indicates why. This is only notnull in case connectedStatus==DISCONNECTED.", example = "CONSENT_EXPIRED")
    FailureReason lastDataFetchFailureReason;

    @NonNull
    @Schema(required = false)
    SiteDTO site;

    @Nullable
    @Schema(description = "Timestamp of the latest successful data fetch.  Value is null prior to a successful data fetch.")
    Instant lastDataFetchTime;

    @NonNull
    @Schema(description = "Consent was given by the PSU at this time.", required = true)
    Instant consentValidFrom;

    /**
     * This feature has been introduced with YCO-823.  The field exposes the 'region' selected by an end-user for banks
     * that require a region (primarily French banks).  We store the region internally and use it to auto-complete
     * the region selection step upon reconsent so that end-users don't have to keep filling in the same information.
     *
     * For clients however there is no value add to having access to this information. The original rationale was that
     * clients could use this field in case they wanted to set up a PIS payment request.  Should this become necessary
     * at some point we can always add this back in, but until now this field has only causes confusion (which is a
     * negative value add).
     */
    @Deprecated
    Map<String, String> metaData;

    @Value
    @Schema(description = "This object will be fetched when you add the request param fetchObject=site if it is supported by the endpoint")
    public static class SiteDTO {
        @NonNull
        @Schema(required = true, example = "123")
        UUID id;
        @NonNull
        @Schema(required = true, example = "ING Bank")
        String name;
        @ArraySchema(arraySchema = @Schema(required = true, description = "The account types this site supports", example = "CURRENT_ACCOUNT,SAVINGS_ACCOUNT,CREDIT_CARD"))
        List<AccountType> supportedAccountTypes;
    }
}
