package nl.ing.lovebird.sitemanagement.accessmeans;

import com.yolt.securityutils.crypto.SecretKey;
import lombok.experimental.UtilityClass;
import nl.ing.lovebird.sitemanagement.providerclient.AccessMeansDTO;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * This class can map between the following types:
 *
 * <ul>
 * <li>{@link AccessMeansDTO} is used to communicate between the {@code providers} and {@code site-management} services</li>
 * <li>{@link AccessMeans} are means with which a user can login at a Provider of type SCRAPING(deprecated)</li>
 * <li>{@link UserSiteAccessMeans} are means with which a user can identify himself at a Provider of type DIRECT_CONNECTION</li>
 * </ul>
 */
@UtilityClass
class AccessMeansMapper {

    public static final Date FAR_FUTURE = Date.from(Instant.parse("3000-01-01T00:00:00Z"));

    public AccessMeans dtoToAccessMeans(final AccessMeansDTO accessMeans, final String provider, final SecretKey encryptionKey) {
        String encryptedAccessMeans = AesEncryptionUtil.encrypt(accessMeans.getAccessMeansBlob(), encryptionKey);
        return new AccessMeans(accessMeans.getUserId(), provider, encryptedAccessMeans, accessMeans.getUpdated(),
                accessMeans.getExpireTime());
    }

    public UserSiteAccessMeans dtoToUserSiteAccessMeans(final AccessMeansDTO accessMeans, final String provider,
                                                        final UUID userSiteId, final SecretKey encryptionKey) {
        String encryptedAccessMeans = AesEncryptionUtil.encrypt(accessMeans.getAccessMeansBlob(), encryptionKey);
        return new UserSiteAccessMeans(
                accessMeans.getUserId(),
                userSiteId,
                provider,
                encryptedAccessMeans,
                accessMeans.getUpdated(),
                accessMeans.getExpireTime(),
                // providers does not know about 'created', so we map it to null
                null
        );
    }

    public AccessMeansDTO accessMeansToDTO(final AccessMeans accessMeans, final SecretKey encryptionKey) {
        String decryptedAccessMeans = AesEncryptionUtil.decrypt(accessMeans.getAccessMeans(), encryptionKey);

        return new AccessMeansDTO(accessMeans.getUserId(), decryptedAccessMeans, accessMeans.getUpdated(),
                limitExpireTime(accessMeans.getExpireTime()));
    }

    public AccessMeansDTO userSiteAccessMeansToDTO(final UserSiteAccessMeans userSiteAccessMeans, final SecretKey encryptionKey) {
        String decryptedAccessMeans = AesEncryptionUtil.decrypt(userSiteAccessMeans.getAccessMeans(), encryptionKey);
        return new AccessMeansDTO(userSiteAccessMeans.getUserId(), decryptedAccessMeans,
                userSiteAccessMeans.getUpdated(), limitExpireTime(userSiteAccessMeans.getExpireTime()));
    }

    /**
     * There are some very large dates in the database. (new Date(Long.MAX_VALUE)).
     * Unfortunately, Jackson isn't able to serialize/deserialize those. That's we why we truncate it to some
     * other very large number/date.
     */
    private static Date limitExpireTime(Date expireTime) {
        return expireTime.after(FAR_FUTURE) ? FAR_FUTURE : expireTime;
    }
}
