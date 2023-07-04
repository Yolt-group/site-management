package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class EncryptionDetailsDTO {
    public static final EncryptionDetailsDTO NONE = new EncryptionDetailsDTO(null);

    private final JWEDetailsDTO jweDetails;

    /**
     * For BudgetInsight and embedded flow
     */
    @Data
    public static class JWEDetailsDTO {
        /**
         * @deprecated Budget Insight specific - use 'alg' from RsaPublicJWK
         */
        @Deprecated
        final String algorithm; // RSA-OAEP
        final String encryptionMethod; // A256GCM
        /**
         * @deprecated Budget Insight specific - JSON with key details sent as a string
         */
        @Deprecated
        final String publicJSONWebKey;
        final RsaPublicJWK rsaPublicJwk;
    }

    /**
     * For embedded flow
     */
    @Data
    public static class RsaPublicJWK {
        final String alg;
        final String kty;
        final String n;
        final String e;
    }
}
