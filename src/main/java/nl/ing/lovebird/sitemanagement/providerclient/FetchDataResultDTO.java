package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.Value;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatusValue;

import java.util.UUID;

@Value
public class FetchDataResultDTO {
    UUID providerRequestId;
    ProviderServiceResponseStatusValue providerServiceResponseStatus;
}
