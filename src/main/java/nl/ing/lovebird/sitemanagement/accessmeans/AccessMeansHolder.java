package nl.ing.lovebird.sitemanagement.accessmeans;

import com.yolt.securityutils.crypto.SecretKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.providerclient.AccessMeansDTO;

import java.time.Instant;
import java.util.Date;

/**
 * Contain either: {@link AccessMeans} or {@link UserSiteAccessMeans}.
 * <p>
 * Can also decrypt the accessMeans and convert to a {@link AccessMeansDTO}
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessMeansHolder {

    public static final Date FAR_FUTURE = Date.from(Instant.parse("3000-01-01T00:00:00Z"));

    AccessMeans accessMeans;
    UserSiteAccessMeans userSiteAccessMeans;
    SecretKey key;

    public static AccessMeansHolder fromAccessMeans(@NonNull AccessMeans accessMeans, @NonNull SecretKey key) {
        return new AccessMeansHolder(accessMeans, null, key);
    }

    public static AccessMeansHolder fromUserSiteAccessMeans(@NonNull UserSiteAccessMeans accessMeans, @NonNull SecretKey key) {
        return new AccessMeansHolder(null, accessMeans, key);
    }

    public AccessMeansDTO toAccessMeansDTO() {
        if (userSiteAccessMeans != null) {
            return userSiteAccessMeansToDTO(userSiteAccessMeans, key);
        } else if (accessMeans != null) {
            return accessMeansToDTO(accessMeans, key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static AccessMeansDTO accessMeansToDTO(final AccessMeans accessMeans, final SecretKey encryptionKey) {
        String decryptedAccessMeans = AesEncryptionUtil.decrypt(accessMeans.getAccessMeans(), encryptionKey);

        return new AccessMeansDTO(accessMeans.getUserId(), decryptedAccessMeans, accessMeans.getUpdated(),
                limitExpireTime(accessMeans.getExpireTime()));
    }

    private static AccessMeansDTO userSiteAccessMeansToDTO(final UserSiteAccessMeans userSiteAccessMeans, final com.yolt.securityutils.crypto.SecretKey encryptionKey) {
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
