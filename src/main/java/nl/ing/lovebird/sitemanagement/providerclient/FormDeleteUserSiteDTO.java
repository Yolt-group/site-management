package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.util.UUID;

@Data
@AllArgsConstructor
public class FormDeleteUserSiteDTO {

    private final String accessMeans;
    private final String userSiteExternalId;
    private final UUID userId;
    private final ClientId clientId;
}
