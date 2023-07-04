package nl.ing.lovebird.sitemanagement.usersite;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import nl.ing.lovebird.sitemanagement.site.LoginType;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class FormLogin extends Login {

    private final FilledInUserSiteFormValues filledInUserSiteFormValues;

    /**
     * Populated if clients provide it in the 'new' way of posting a form.
     */
    private final UUID stateId;

    private final LoginType loginType = LoginType.FORM;

    public FormLogin(final UUID userId,
                     final FilledInUserSiteFormValues filledInUserSiteFormValues, final UUID stateId) {
        super(userId);
        this.filledInUserSiteFormValues = filledInUserSiteFormValues;
        this.stateId = stateId;
    }

}
