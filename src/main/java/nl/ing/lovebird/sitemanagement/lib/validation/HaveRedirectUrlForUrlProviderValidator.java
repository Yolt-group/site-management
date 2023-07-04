package nl.ing.lovebird.sitemanagement.lib.validation;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

@Slf4j
public class HaveRedirectUrlForUrlProviderValidator implements ConstraintValidator<HaveRedirectUrlForUrlProvider, PostgresUserSite> {

    @Override
    public boolean isValid(PostgresUserSite userSite, ConstraintValidatorContext context) {
        try {
            if (!isScrapingSite(userSite.getProvider()) && userSite.getRedirectUrlId() == null) {
                return false;
            }
            if (isScrapingSite(userSite.getProvider()) && userSite.getRedirectUrlId() != null) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Validation of usersite failed. userId: {} userSiteId: {}", userSite.getUserId(), userSite.getUserSiteId(), ex);
            return false;
        }
    }

}
