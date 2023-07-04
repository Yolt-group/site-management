package nl.ing.lovebird.sitemanagement.exception;

import lombok.Getter;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.ServiceType;

import java.util.UUID;

public class ProviderNotEnabledForServiceTypeException extends RuntimeException {

    @Getter
    private final ServiceType serviceType;

    public ProviderNotEnabledForServiceTypeException(ClientToken clientToken, UUID siteId, ServiceType serviceType) {
        super(String.format(
                "ClientConfiguration problem: %s client=%s is trying to use site=%s for service type=%s but the provider is not onboarded.",
                clientToken.isPSD2Licensed() ? "licensed" : "non licensed",
                clientToken.getClientIdClaim().toString(),
                siteId,
                serviceType));
        this.serviceType = serviceType;
    }
}