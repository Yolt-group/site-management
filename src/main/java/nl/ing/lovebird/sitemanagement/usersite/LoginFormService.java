package nl.ing.lovebird.sitemanagement.usersite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.UnknownSiteFormException;
import nl.ing.lovebird.sitemanagement.forms.LoginFormParser;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerclient.FormFetchLoginDTO;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.LoginFormResponseDTO;
import nl.ing.lovebird.sitemanagement.sites.Site;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.GET_STEP;

@RequiredArgsConstructor
@Service
@Slf4j
public class LoginFormService {
    private final SiteLoginFormRepository siteLoginFormRepository;
    private final FormProviderRestClient formProviderRestClient;
    private final Clock clock;
    private final SiteManagementMetrics siteManagementMetrics;

    /**
     * Get Login form for a specified site.
     * <p> First of all, this method loads the form from the database and checks the updated date.
     * If more than one day has passed, the method fetches the form from the provider.
     * <p>If the form from the provider is different from the form from the database,
     * the new form is saved to the database, and the alternate form is discarded.
     * <p>If the form from the provider matches the form from the database, the updated date is updated in the database.
     *
     * @param site     The site
     * @param clientUserToken The client token for authorization
     * @return Login form for specified site
     * @throws UnknownSiteFormException if no login form is database and fetching login form provider is failed
     */
    public SiteLoginForm getLoginForm(final Site site, final ClientUserToken clientUserToken) {
        SiteLoginForm currentLoginForm = siteLoginFormRepository.selectSiteLogin(site.getId());

        if (currentLoginForm != null && !isStale(currentLoginForm)) {
            return currentLoginForm;
        }

        Optional<SiteLoginForm> fetched = fetch(site, new ClientId(clientUserToken.getClientIdClaim()), clientUserToken);
        if (fetched.isEmpty()) {
            if (currentLoginForm != null) {
                return currentLoginForm;
            } else {
                throw new UnknownSiteFormException(String.format("No site form found for this siteId: %s", site.getId()));
            }
        }

        SiteLoginForm newLoginForm = fetched.get();
        if (shouldUpdateForm(currentLoginForm, newLoginForm)) {
            currentLoginForm = newLoginForm;
            log.warn("Alternative to {} form for site {} has now been discarded", site.getProvider(), site.getId());
        } else {
            currentLoginForm.setUpdated(clock.instant());
        }
        siteLoginFormRepository.save(currentLoginForm);
        log.info("Form for site {} , {} has been updated with new form from {}", site.getId(), site.getName(), site.getProvider());

        return currentLoginForm;
    }


    private boolean shouldUpdateForm(SiteLoginForm storedSiteLoginForm, SiteLoginForm fetchedLoginForm) {
        // Update whenever there's no raw form available yet to initialize to the scraping partner provided form
        if (storedSiteLoginForm == null
                || StringUtils.isEmpty(storedSiteLoginForm.getLoginFormJson())
                || StringUtils.isEmpty(storedSiteLoginForm.getLoginForm())) {
            return true;
        }

        // Never update when the newly retrieved form is empty; That'd be a provider error we don't want to push to our users
        if (StringUtils.isEmpty(fetchedLoginForm.getLoginFormJson())
                || StringUtils.isEmpty(fetchedLoginForm.getLoginForm())) {
            return false;
        }

        // Update when the stored form differs from the new form
        return !storedSiteLoginForm.getLoginFormJson().equals(fetchedLoginForm.getLoginFormJson());
    }

    private Optional<SiteLoginForm> fetch(final Site site, final ClientId clientId, final ClientUserToken clientUserToken) {
        String provider = site.getProvider();
        try (LogBaggage b = LogBaggage.builder().siteId(site.getId()).build()) {
            FormFetchLoginDTO formFetchLoginDTO = new FormFetchLoginDTO(site.getExternalId(), clientId);
            LoginFormResponseDTO loginFormResponse = formProviderRestClient.fetchLoginForm(provider, formFetchLoginDTO, clientUserToken, site.getId());
            String rawLoginForm = loginFormResponse.getProviderForm();
            String convertedLoginForm = LoginFormParser.writeForm(loginFormResponse.getYoltForm());
            return Optional.of(new SiteLoginForm(site.getId(), rawLoginForm, null, convertedLoginForm, clock.instant()));
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(GET_STEP, provider, e);
            log.warn("Failed to fetch login form for site {}", site.getId(), e);
            return Optional.empty();
        } catch (RuntimeException | IOException e) {
            log.error("Failed to fetch login form for site {}", site.getId(), e);
            return Optional.empty();
        }
    }

    private boolean isStale(SiteLoginForm form) {
        return form.getUpdated() == null || form.getUpdated().isBefore(clock.instant().minus(1, ChronoUnit.DAYS));
    }
}
