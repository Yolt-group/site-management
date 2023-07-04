package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.SCR_GET_ENCRYPTION_DETAILS;

@RequiredArgsConstructor
@Service
public class EncryptionDetailsService {
    private final Map<ProviderClientEntry, EncryptionDetailsDTO> perProviderClientEncryptionDetailsCache = new HashMap<>();

    @Value("${lovebird.cacheEncryptionDetails}")
    private boolean cacheEncryptionDetails;

    private final FormProviderRestClient formProviderRestClient;
    private final SiteManagementMetrics siteManagementMetrics;

    public EncryptionDetailsDTO getEncryptionDetails(
            final UUID siteId,
            final String provider,
            final ClientToken clientToken
    ) {
        if (SiteService.isSiteStubbedByYoltbank(siteId)) {
            EncryptionDetailsDTO encryptionDetails = getYoltbankEncryptionDetails(provider);
            if (encryptionDetails != null) {
                return encryptionDetails;
            }
        }
        EncryptionDetailsDTO encryptionDetails = perProviderClientEncryptionDetailsCache.get(new ProviderClientEntry(provider, new ClientId(clientToken.getClientIdClaim())));
        if (cacheEncryptionDetails && encryptionDetails != null) {
            return encryptionDetails;
        }
        try {
            return getFormDataProviderEncryptionDetails(provider, clientToken);
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_GET_ENCRYPTION_DETAILS, provider, e);
            // No way to recover.
            throw new KnownProviderRestClientException(e);
        }
    }

    private EncryptionDetailsDTO getYoltbankEncryptionDetails(final String provider) {
        if (provider.equals("BUDGET_INSIGHT")) {
            return new EncryptionDetailsDTO(new EncryptionDetailsDTO.JWEDetailsDTO("RSA-OAEP", "A256GCM", "{\"alg\": \"RSA-OAEP\", \"e\": \"AQAB\"," +
                    " \"kid\": \"zvGSMN77LnUOC1F99UHkN0oY1pmINzqpyTCaGU34ebk\", \"kty\": \"RSA\", \"n\":" +
                    " \"z3tsg23PFL3YJ5i8UtR-Ac0RbYbzX9Tb5EQOiFrleT1YLbxXezZoymKQm5VKpNpOxVHieLPJ" +
                    "-uwFfKLHMKvQ67T6CxVksErXMld1K5vG9RSm7wy1KplcbuczW5vXD1I5Gp2Ss7QQbZr5C7IN3pCKgvL9QuAqsc1dszVtajMWwN3WZCxx7Jq1n" +
                    "-xdNCpsATZKY596l4dBOk8x4rzGRRJ1KcfYCHOMUVmFVIV7pOn89hI3HlhqsyevAzXz4m-2kiSfHJGCyzQ8Z410wXjPNLR9K-Nm8vo69_DFc9Z7gtnJ5OPpEgO6l2OwT6I6-ez" +
                    "-y6qFUmIFGtBbQbxaYpF51nGJPSNbH_URDKHNc_sF4saE_WBa9nTJj4GfW7dX_MBE4CHqd0MiUIO7_HYpUleiAVOtGLEdetNVBQh-W4Yd7-d7rjTMT9oAKM-bzCBL4HftsmSfmiW9BzuwUEQs6" +
                    "-o1EnQM3RCZj6X58U2fxPWsT4GtTDSjuGWQY_RfDkAsdYT_MMYr2FBka1GPiTyU-oKIvE910Q1OQx_q_lQN-2gmO2tcpu7HLUklHXsOIwBczsZfrhGcQq5TxOmLPmADk7nhS7d" +
                    "-pD8pWma3D8NrJh8YtG1YsqZA858DxkjS4VGsGvBBb2DwESFxq4bZwo0zhJ7lMhuxi2kxjWxtXD-xvPW-mLyOKnc\", \"use\": \"enc\"}", null));
        }
        return null;
    }

    private EncryptionDetailsDTO getFormDataProviderEncryptionDetails(
            final String provider,
            final ClientToken clientToken
    ) throws HttpException {
        EncryptionDetailsDTO encryptionDetails = formProviderRestClient.getEncryptionDetails(provider, clientToken);
        perProviderClientEncryptionDetailsCache.put(new ProviderClientEntry(provider, new ClientId(clientToken.getClientIdClaim())), encryptionDetails);
        return encryptionDetails;
    }


    @Data
    static class ProviderClientEntry {
        private final String provider;
        private final ClientId clientId;
    }
}
