package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.usersite.encryption.JWEFormStepEncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.usersite.encryption.NoFormStepEncryptionDetailsDTO;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = NoFormStepEncryptionDetailsDTO.TYPE, value = NoFormStepEncryptionDetailsDTO.class),
        @JsonSubTypes.Type(name = JWEFormStepEncryptionDetailsDTO.TYPE, value = JWEFormStepEncryptionDetailsDTO.class),
})
@Schema(name = "EncryptionDetails",
        description = "Configuration details describing the encryption algorithm.",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = NoFormStepEncryptionDetailsDTO.TYPE, schema = NoFormStepEncryptionDetailsDTO.class),
                @DiscriminatorMapping(value = JWEFormStepEncryptionDetailsDTO.TYPE, schema = JWEFormStepEncryptionDetailsDTO.class),
        },
        oneOf = {
                NoFormStepEncryptionDetailsDTO.class,
                JWEFormStepEncryptionDetailsDTO.class,
        }
)
public interface FormStepEncryptionDetailsDTO {

    @Schema(description = "Encryption type; use this to switch between alternative implementations", required = true)
    Type getType();

    enum Type {
        JWE,
        NONE,
    }

    /**
     * Convert internal EncryptionDetails model to an external DTO model. This allows us to change the internal structure
     * without updating the client models.
     *
     * @param encryptionDetails internal model of encryption details
     * @return external model of encryption details
     */
    static FormStepEncryptionDetailsDTO from(EncryptionDetailsDTO encryptionDetails) {
        final boolean jwe = encryptionDetails.getJweDetails() != null;

        if (!jwe) {
            return new NoFormStepEncryptionDetailsDTO();
        }

        JWEFormStepEncryptionDetailsDTO.RsaPublicJwkDTO rsaPublicJwkDTO = null;
        if (encryptionDetails.getJweDetails().getRsaPublicJwk() != null) {
            rsaPublicJwkDTO = new JWEFormStepEncryptionDetailsDTO.RsaPublicJwkDTO(
                    encryptionDetails.getJweDetails().getRsaPublicJwk().getAlg(),
                    encryptionDetails.getJweDetails().getRsaPublicJwk().getKty(),
                    encryptionDetails.getJweDetails().getRsaPublicJwk().getN(),
                    encryptionDetails.getJweDetails().getRsaPublicJwk().getE()
            );
        }

        // BudgetInsight and embedded flow currently uses JWE
        final JWEFormStepEncryptionDetailsDTO.JWEDetailsDTO jweDetails = new JWEFormStepEncryptionDetailsDTO.JWEDetailsDTO(
                encryptionDetails.getJweDetails().getAlgorithm(),
                encryptionDetails.getJweDetails().getEncryptionMethod(),
                encryptionDetails.getJweDetails().getPublicJSONWebKey(),
                rsaPublicJwkDTO
        );
        return new JWEFormStepEncryptionDetailsDTO(jweDetails);
    }

}

