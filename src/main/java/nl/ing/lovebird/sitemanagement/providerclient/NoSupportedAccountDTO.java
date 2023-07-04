package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class NoSupportedAccountDTO {
    private final UUID userId;
    private final UUID userSiteId;
    private final UUID providerRequestId;
}
