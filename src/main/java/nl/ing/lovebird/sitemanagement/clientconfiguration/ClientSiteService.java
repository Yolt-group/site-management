package nl.ing.lovebird.sitemanagement.clientconfiguration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.exception.ClientSiteNotEnabledException;
import nl.ing.lovebird.sitemanagement.exception.ProviderNotEnabledForServiceTypeException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.types.SiteId;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSiteService {


    private final ClientSitesProvider clientSitesProvider;
    private final ClientRedirectUrlService clientRedirectUrlService;

    public boolean isClientUsingExperimentalVersion(ClientId clientId, UUID siteId) {
        return clientSitesProvider.getClientSite(clientId, new SiteId(siteId))
                .map(ClientSiteDTO::isUseExperimentalVersion).orElse(false);
    }

    public void validateIsClientSiteEnabledForServiceTypeAndRedirectUrlId(ClientToken clientToken, UUID siteId, UUID redirectUrlId, ServiceType serviceType) {
        Optional<ClientSiteDTO> optionalClientSite = clientSitesProvider.getClientSite(new ClientId(clientToken.getClientIdClaim()), new SiteId(siteId));
        if (optionalClientSite.isEmpty()) {
            throw new ClientSiteNotEnabledException(String.format("ClientConfiguration problem: client=%s is trying to use site=%s for serviceType=%s, but there is no ClientSite.",
                    clientToken.getClientIdClaim(), siteId, serviceType));
        }
        ClientSiteDTO clientSiteDTO = optionalClientSite.get();

        ClientSiteDTO.Services.Onboarded onboardedForServiceType = switch (serviceType) {
            case AIS -> clientSiteDTO.getServices().getAis() != null ? clientSiteDTO.getServices().getAis().getOnboarded() : null;
            case PIS, AS, IC -> throw new IllegalArgumentException("Service type " + serviceType + " not supported");
        };
        if (onboardedForServiceType == null) {
            throw new ProviderNotEnabledForServiceTypeException(clientToken, siteId, serviceType);
        }

        // Special case.
        // The client is an unlicensed client, and the redirectUrlId is the "magic" redirectUrlId for the
        // consent-starter that is available to all unlicensed clients (without having to be explicitly configured as a
        // redirectUrl at the client-level).
        if (!clientToken.isPSD2Licensed() && ClientRedirectUrlService.CONSENT_STARTER_REDIRECT_URL_ID.equals(redirectUrlId)) {
            return;
        }

        if (redirectUrlId != null && !onboardedForServiceType.getRedirectUrlIds().contains(redirectUrlId)) {
            throw new ProviderNotEnabledForServiceTypeException(clientToken, siteId, serviceType);
        }
    }
}
