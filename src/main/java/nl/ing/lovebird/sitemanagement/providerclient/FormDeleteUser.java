package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

@Data
@AllArgsConstructor
public class FormDeleteUser {

    private final AccessMeansDTO accessMeansDTO;
    private final ClientId clientId;
}
