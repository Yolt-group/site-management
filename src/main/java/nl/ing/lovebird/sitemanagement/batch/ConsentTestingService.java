package nl.ing.lovebird.sitemanagement.batch;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.InvokeConsentTestingDTO;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentTestingService {

    private final ProviderRestClient providerRestClient;
    private final ClientRedirectUrlService clientRedirectUrlService;
    private final AuthenticationMeansFactory authenticationMeansFactory;

    public void invokeConsentTests(UUID clientRedirectUrlId, ClientToken clientToken, ServiceType serviceType) throws HttpException {
        String baseClientRedirectUrl = clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(clientToken, clientRedirectUrlId);
        AuthenticationMeansReference authenticationMeansReference = authenticationMeansFactory.createAuthMeans(clientToken, clientRedirectUrlId);
        InvokeConsentTestingDTO invokeConsentTestingDTO = new InvokeConsentTestingDTO(baseClientRedirectUrl, authenticationMeansReference);
        providerRestClient.invokeConsentTests(invokeConsentTestingDTO, clientToken, serviceType);
    }
}
