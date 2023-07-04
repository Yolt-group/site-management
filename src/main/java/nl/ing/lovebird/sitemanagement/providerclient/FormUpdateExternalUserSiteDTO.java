package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import nl.ing.lovebird.providershared.form.FormSiteLoginFormDTO;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class FormUpdateExternalUserSiteDTO {
    private final String externalSiteId;
    private final FormUserSiteDTO formUserSite;
    private final String accessMeans;
    private final Instant transactionsFetchStartTime;
    private final FilledInUserSiteFormValues filledInUserSiteFormValues;
    private final FormSiteLoginFormDTO formSiteLoginForm;
    private final ClientId clientId;
    private final UUID providerRequestId;
    private final UserSiteDataFetchInformation userSiteDataFetchInformation;
    private final UUID siteId;
    private final UUID activityId;
}
