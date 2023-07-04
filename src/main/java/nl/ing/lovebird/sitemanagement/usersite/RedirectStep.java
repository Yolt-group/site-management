package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Getter
@Setter
@Schema(name = "RedirectStep", description = "A step representing the action of redirecting the user to a given URL.")
public class RedirectStep extends Step {

    @Schema(description = "The URL the user should be redirected to")
    private final String redirectUrl;

    @Schema(hidden = true)
    private final String externalConsentId;

    /**
     * A stateId. A 'form' is presented when the user adds or updates a user-site. The stateId should come from the 'usersite session'.
     * (i.e. the session where he/she adds the user-site)
     * This stateId should be send back on POST /user-sites, so we know what we were doing by getting the correct user site session.
     */
    private UUID stateId;

    @JsonCreator
    public RedirectStep(@NonNull @JsonProperty("redirectUrl") String redirectUrl,
                        @Nullable @JsonProperty("providerState") String providerState,
                        @Nullable @JsonProperty("externalConsentId") String externalConsentId,
                        @Nullable @JsonProperty("stateId") UUID stateId) {
        super(providerState);
        this.redirectUrl = redirectUrl;
        this.externalConsentId = externalConsentId;
        this.stateId = stateId;
    }
}
