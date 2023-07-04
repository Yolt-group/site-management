package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

@Value
@Schema(name = "RedirectStepObject", description = "A redirect URL step in the login flow that the PSU needs to be navigated to.")
public class RedirectStepObject {
    @Schema(description = "The URL the user should be redirected to.", required = true)
    String url;
    @Schema(description = "The state parameter that can be used to relate the bank's redirectUrl to this user-site.", required = true)
    String state;
}
