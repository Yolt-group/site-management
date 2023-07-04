package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FormCreateNewUserRequestDTO {

    private final UUID userId;
    private final UUID siteId;
    private final ClientId clientId;

    /**
     * Create User is the only operation where there is no user nor site-context so this boolean is only used for stubbing purposes.
     * Yoltbank will not proxy a request to the actual provider when the header 'Test-User=true'.
     * This is not elegant solution, but allows us to use testusers and real users at the same environment on the same time.
     */
    @JsonProperty(value="isTestUser")
    private final boolean isTestUser;
}
