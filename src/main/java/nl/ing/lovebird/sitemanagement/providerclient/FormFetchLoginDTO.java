package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

@Data
@AllArgsConstructor
public class FormFetchLoginDTO {

    private final String externalSiteId;
    private final ClientId clientId;


}
