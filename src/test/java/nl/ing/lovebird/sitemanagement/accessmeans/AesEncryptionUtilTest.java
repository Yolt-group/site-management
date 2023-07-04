package nl.ing.lovebird.sitemanagement.accessmeans;

import com.yolt.securityutils.crypto.SecretKey;
import nl.ing.lovebird.sitemanagement.accessmeans.AesEncryptionUtil;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AesEncryptionUtilTest {

    public static final SecretKey secretKey = SecretKey.from(Hex.decode("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"));

    @Test
    void encryptAndDecryptLeadsToSameValue() throws Exception {
        String input = "thisIsASecretString";
        String encryptedString = AesEncryptionUtil.encrypt(input, secretKey);
        assertThat(encryptedString).isNotEqualTo(input);
        String decryptedString = AesEncryptionUtil.decrypt(encryptedString, secretKey);
        assertThat(decryptedString).isEqualTo(input);
    }

}