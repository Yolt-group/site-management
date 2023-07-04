package nl.ing.lovebird.sitemanagement.usersite.encryption;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.usersite.FormStepEncryptionDetailsDTO;

@Schema(name = "JWEFormStepEncryptionDetails")
public record JWEFormStepEncryptionDetailsDTO(@Schema(required = true) JWEDetailsDTO jweDetails) implements FormStepEncryptionDetailsDTO {

    public static final String TYPE = "JWE";

    @Override
    @Schema(type = "string", allowableValues = TYPE)
    public Type getType() {
        return Type.JWE;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @Schema(name = "JWEDetails", description = "JSON Web Encryption configuration")
    public static class JWEDetailsDTO {
        /**
         * @deprecated Budget Insight specific -  use 'alg' from RsaPublicJWK
         */
        @Deprecated
        @Hidden
        @Schema(description = "JSON Web Encryption encryption algorithm to encrypt the Content Encryption Key (CEK), i.e. the 'alg' header of a JWE")
        String algorithm;
        @Schema(description = "JSON Web Content Encryption Algorithm (CEK). i.e. the 'enc' header of a JWE", required = true)
        String encryptionMethod;
        /**
         * @deprecated Budget Insight specific - JSON with key details sent as a string
         */
        @Deprecated
        @Hidden
        @Schema(description = "JSON Web Key with which to encrypt credentials")
        String publicJSONWebKey;
        @Schema(description = "RSA JSON Web Public Key which should be used to create the encrypted values in the form of a JWE", required = true)
        RsaPublicJwkDTO rsaPublicJWK;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @Schema(name = "RsaPublicJWK", description = "RSA JSON Web Public Key")
    public static class RsaPublicJwkDTO {
        @Schema(description = "The specific cryptographic algorithm used with the key", required = true)
        String alg;
        @Schema(description = "The family of cryptographic algorithms used with the key", required = true)
        String kty;
        @Schema(description = "The modulus for the RSA public key", required = true)
        String n;
        @Schema(description = "The exponent for the RSA public key", required = true)
        String e;
    }
}
