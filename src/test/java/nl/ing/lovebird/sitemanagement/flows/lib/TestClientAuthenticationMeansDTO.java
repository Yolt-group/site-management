package nl.ing.lovebird.sitemanagement.flows.lib;

import lombok.Value;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.util.UUID;

/**
 * Identical to ClientAuthenticationMeansDTO, copied because it is package private.
 *
 * @deprecated to discourage usage
 */
@Value
@Deprecated
public class TestClientAuthenticationMeansDTO {
    ClientId clientId;
    UUID redirectUrlId;
    String provider;
    ServiceType serviceType;
}
