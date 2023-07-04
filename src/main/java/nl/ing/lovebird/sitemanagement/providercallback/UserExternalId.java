package nl.ing.lovebird.sitemanagement.providercallback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/**
 *
 * SCRAPERS ONLY
 *
 * Entity that allows us to map an internal Yolt user id to a external provider user id.
 * Used to lookup the provider user id when retrieving data for a specific Yolt user, should NOT be removed.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_external_id")
@IdClass(UserExternalId.Id.class)
public class UserExternalId {


    @javax.persistence.Id
    @Column(name = "user_id")
    private UUID userId;

    @javax.persistence.Id
    @Column(name = "provider")
    private String provider;

    @Column(name = "external_user_id")
    private String externalUserId;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {
        UUID userId;
        String provider;
    }
}
