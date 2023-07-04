package nl.ing.lovebird.sitemanagement.batch;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.InvokeConsentTestingDTO;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentTestingServiceTest {

    private static final UUID CLIENT_REDIRECT_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final ClientToken CLIENT_TOKEN = new ClientToken("some_serialized_token", new JwtClaims());
    private static final String REDIRECT_URL = "http://redirecturl.com";
    private static final AuthenticationMeansReference AUTHENTICATION_MEANS_REFERENCE = new AuthenticationMeansReference(CLIENT_ID, null, CLIENT_REDIRECT_ID);

    @Mock
    private ProviderRestClient providerRestClient;

    @Mock
    private ClientRedirectUrlService clientRedirectUrlService;

    @Mock
    private AuthenticationMeansFactory authenticationMeansFactory;

    @InjectMocks
    private ConsentTestingService consentTestingService;

    @Test
    void shouldInvokeConsentTests() throws HttpException {
        //Given
        when(clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(CLIENT_TOKEN, CLIENT_REDIRECT_ID)).thenReturn(REDIRECT_URL);
        when(authenticationMeansFactory.createAuthMeans(CLIENT_TOKEN, CLIENT_REDIRECT_ID)).thenReturn(AUTHENTICATION_MEANS_REFERENCE);
        InvokeConsentTestingDTO invokeConsentTestingDTO = new InvokeConsentTestingDTO(REDIRECT_URL, AUTHENTICATION_MEANS_REFERENCE);

        //When
        consentTestingService.invokeConsentTests(CLIENT_REDIRECT_ID, CLIENT_TOKEN, ServiceType.AIS);

        //Then
        verify(providerRestClient).invokeConsentTests(invokeConsentTestingDTO, CLIENT_TOKEN, ServiceType.AIS);
    }
}