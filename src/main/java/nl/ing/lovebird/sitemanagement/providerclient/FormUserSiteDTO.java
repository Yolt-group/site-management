package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormUserSiteDTO {

    private UUID userId;
    private UUID userSiteId;
    private String externalId;
    /**
     * @deprecated
     * This is site management specific and should not be propagated to the
     * providers layer/service.
     * FormUserSiteDTO should only be used as an interface between site management and providers.
     * Need to refactor usage of FormUserSiteDTO.
     * See issue 1285
     */
    private Date lastDataFetch;

    public FormUserSiteDTO(final UUID userId, final UUID userSiteId, final String externalId) {
        this.userId = userId;
        this.userSiteId = userSiteId;
        this.externalId = externalId;
    }
}
