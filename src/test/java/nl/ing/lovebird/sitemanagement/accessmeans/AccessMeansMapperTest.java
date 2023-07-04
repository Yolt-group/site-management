package nl.ing.lovebird.sitemanagement.accessmeans;

import com.yolt.securityutils.crypto.SecretKey;
import nl.ing.lovebird.sitemanagement.providerclient.AccessMeansDTO;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansMapper.*;
import static nl.ing.lovebird.sitemanagement.accessmeans.AesEncryptionUtil.decrypt;
import static nl.ing.lovebird.sitemanagement.accessmeans.AesEncryptionUtil.encrypt;
import static org.assertj.core.api.Assertions.assertThat;

public class AccessMeansMapperTest {

    private static final Date YEAR_3000 = Date.from(Instant.parse("3000-01-01T00:00:00Z"));
    private static final SecretKey secretKey = SecretKey.from("a3f60fafc948035382fbe9ce7b4535c4".getBytes());

    @Test
    void testAccessMeansLargeDate() {
        String encryptedAccessMeans = encrypt("someToken", secretKey);
        AccessMeans accessMeans = new AccessMeans(UUID.randomUUID(), "SALTEDGE", encryptedAccessMeans, new Date(), new Date(Long.MAX_VALUE));

        AccessMeansDTO accessMeansDTO = accessMeansToDTO(accessMeans, secretKey);

        assertThat(accessMeansDTO.getUserId()).isEqualTo(accessMeans.getUserId());
        assertThat(accessMeansDTO.getAccessMeansBlob()).isEqualTo("someToken");
        assertThat(accessMeansDTO.getUpdated()).isCloseTo(accessMeans.getUpdated(), Duration.ofMinutes(1).toMillis());
        assertThat(accessMeansDTO.getExpireTime()).isCloseTo(YEAR_3000, Duration.ofHours(1).toMillis());
    }

    @Test
    void testUserSiteAccessMeansLargeDate() {
        String encryptedAccessMeans = encrypt("someToken", secretKey);

        UserSiteAccessMeans userSiteAccessMeans = new UserSiteAccessMeans(UUID.randomUUID(), UUID.randomUUID(), "STARLINGBANK", encryptedAccessMeans, new Date(), new Date(Long.MAX_VALUE), Instant.EPOCH);

        AccessMeansDTO accessMeansDTO = userSiteAccessMeansToDTO(userSiteAccessMeans, secretKey);

        assertThat(accessMeansDTO.getUserId()).isEqualTo(userSiteAccessMeans.getUserId());
        assertThat(accessMeansDTO.getAccessMeansBlob()).isEqualTo("someToken");
        assertThat(accessMeansDTO.getUpdated()).isCloseTo(userSiteAccessMeans.getUpdated(), Duration.ofMinutes(1).toMillis());
        assertThat(accessMeansDTO.getExpireTime()).isCloseTo(YEAR_3000, Duration.ofHours(1).toMillis());
    }

    @Test
    void testDtoToAccessMeans() {

        AccessMeansDTO accessMeansDTO = new AccessMeansDTO(UUID.randomUUID(), "someToken", new Date(), new Date());

        AccessMeans accessMeans = dtoToAccessMeans(accessMeansDTO, "BUDGET_INSIGHT", secretKey);

        assertThat(accessMeans.getUserId()).isEqualTo(accessMeansDTO.getUserId());
        assertThat(accessMeans.getProvider()).isEqualTo("BUDGET_INSIGHT");
        assertThat(decrypt(accessMeans.getAccessMeans(), secretKey)).isEqualTo("someToken");
        assertThat(accessMeans.getUpdated()).isEqualTo(accessMeansDTO.getUpdated());
        assertThat(accessMeans.getExpireTime()).isEqualTo(accessMeansDTO.getExpireTime());
    }

    @Test
    void testDtoToUserSiteAccessMeans() {
        AccessMeansDTO accessMeansDTO = new AccessMeansDTO(UUID.randomUUID(), "someToken", new Date(), new Date());
        UUID userSiteId = UUID.randomUUID();

        UserSiteAccessMeans userSiteAccessMeans = dtoToUserSiteAccessMeans(accessMeansDTO, "BUDGET_INSIGHT", userSiteId, secretKey);

        assertThat(userSiteAccessMeans.getUserId()).isEqualTo(accessMeansDTO.getUserId());
        assertThat(userSiteAccessMeans.getUserSiteId()).isEqualTo(userSiteId);
        assertThat(userSiteAccessMeans.getProvider()).isEqualTo("BUDGET_INSIGHT");
        assertThat(decrypt(userSiteAccessMeans.getAccessMeans(), secretKey)).isEqualTo("someToken");
        assertThat(userSiteAccessMeans.getUpdated()).isEqualTo(accessMeansDTO.getUpdated());
        assertThat(userSiteAccessMeans.getExpireTime()).isEqualTo(accessMeansDTO.getExpireTime());
    }
}
