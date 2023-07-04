package nl.ing.lovebird.sitemanagement.accessmeans;


import com.yolt.securityutils.crypto.SecretKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.EncoderException;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

/**
 * @deprecated This only exists so we can deal with data that is already stored in cassandra.
 * There are other encryption methods available that are better suited. See security-utils library.
 */
@Deprecated
public class AesEncryptionUtil {

    private static final String PROVIDER = "BC";
    static final String ALGORITHM = "AES";
    private static final String MODE = "GCM";
    private static final String PADDING = "NoPadding";
    private static final String TRANSFORMATION = ALGORITHM + "/" + MODE + "/" + PADDING;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private AesEncryptionUtil() {

    }

    public static String encrypt(String input, SecretKey secretKey) {
        try {
            Key key = secretKey.getKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[256 >> 3];
            secureRandom.nextBytes(iv);
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(inputBytes);
            return new String(Hex.encode(iv)) + new String(Hex.encode(encryptedBytes));
        } catch (EncoderException | DecoderException | NoSuchAlgorithmException |
                InvalidKeyException | NoSuchPaddingException |
                InvalidAlgorithmParameterException | NoSuchProviderException |
                BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("Exception while encrypting", e);
        }
    }

    public static String decrypt(String encrypted, SecretKey secretKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = Hex.decode(encrypted.substring(0, 64));
            byte[] encryptedData = Hex.decode(encrypted.substring(64));
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            Key key = secretKey.getKey();
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (DecoderException | NoSuchAlgorithmException | BadPaddingException | NoSuchPaddingException |
                InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new RuntimeException("Exception while decrypting", e);
        }
    }

}
