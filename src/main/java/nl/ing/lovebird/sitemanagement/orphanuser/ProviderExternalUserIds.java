package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.util.List;
import java.util.UUID;

@Value
public class ProviderExternalUserIds {

    @NonNull ClientId clientId;
    @NonNull UUID batchId;
    @NonNull String provider;
    @NonNull List<String> externalUserIds;
    @NonNull Boolean isLast; // should be reference type (otherwise lombok generates getter/setter with incorrect names)
}
