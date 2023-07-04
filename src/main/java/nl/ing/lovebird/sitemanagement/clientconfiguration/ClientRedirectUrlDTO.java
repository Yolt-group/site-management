package nl.ing.lovebird.sitemanagement.clientconfiguration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(name = "ClientRedirectURL", description = "Holds the client id, the redirect-url id and the url value.")
public class ClientRedirectUrlDTO {

    @Schema(description = "The client-id." +
            " Ignored in POST/PUT (create/update) operation.")
    protected ClientId clientId;

    @Schema(description = "The client-redirect-url-id.")
    protected UUID redirectUrlId;

    @URL
    @NotNull
    @Schema(description = "The URL that should be used to redirect a user back to. (i.e. the url of the browser-app or native-app.")
    protected String url;
}
