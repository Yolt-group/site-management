package nl.ing.lovebird.sitemanagement.externalconsent;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
public class ExternalConsentService {

    private final Clock clock;
    private final ExternalConsentRepository consentRepository;
    private final ConsentExpiryStub consentExpiryStub;
    private final boolean useExternalConsentStub;

    public ExternalConsentService(Clock clock, ExternalConsentRepository consentRepository, ConsentExpiryStub consentExpiryStub,
                                  @Value("${useExternalConsentStub}") boolean useExternalConsentStub) {
        this.clock = clock;
        this.consentRepository = consentRepository;
        this.consentExpiryStub = consentExpiryStub;
        this.useExternalConsentStub = useExternalConsentStub;
    }

    public Optional<ExternalConsent> findById(@NotNull UUID userId, @NotNull UUID siteId, @NotNull UUID userSiteId) {
        return consentRepository
                .findBy(userId, siteId, userSiteId);
    }

    public Optional<Instant> findConsentExpiryBy(@NotNull UUID userId, @NotNull UUID siteId, @NotNull UUID userSiteId) {
        return consentRepository
                .findBy(userId, siteId, userSiteId)
                .map(ExternalConsent::getExpiryTimestamp);
    }

    public void createOrUpdateConsent(@NotNull UUID userId, @NotNull Site site, @NotNull UUID userSiteId, @Nullable String externalConsentId) {

        Integer expiryInDays = site.getConsentExpiryInDays();

        if (useExternalConsentStub) {
            expiryInDays = consentExpiryStub.consentExpiryForUser(userId, site);
        }

        if (expiryInDays == null) {
            // Not allowed to insert NULL in a primary key in Cassandra, so we have to do nothing in this case.
            // If there is a bank which needs this functionality, but has an infinite consent, we can always configure something like 365 days.
            return;
        }

        Instant now = Instant.now(clock);
        Instant consentExpiryDate = now.plus(expiryInDays, ChronoUnit.DAYS);
        String consentExpiryWeek = ConsentExpiryTimeFormatter.format(consentExpiryDate);

        ExternalConsent consent = new ExternalConsent(userId, site.getId(), userSiteId, consentExpiryWeek, consentExpiryDate, now, externalConsentId);
        consentRepository.persist(consent);
    }

    public void deleteForUserSite(@NotNull UUID userId, @NotNull UUID siteId, @NotNull UUID userSiteId) {
        consentRepository.delete(userId, siteId, userSiteId);
    }

    /**
     * Returns a consentId, but *only* if it's already valid. If we're using an 'expired' externalConsent, the 'improved authorization flow'
     * at the bank fails. We should only provide a valid external consent, so the user can go through an improved *RE*-consent flow.
     * Otherwise we'll just fallback to the normal 'consent flow' (not *RE*consent)
     */
    public String getExternalConsentIdForValidExternalConsent(PostgresUserSite userSite) {
        return findById(userSite.getUserId(), userSite.getSiteId(), userSite.getUserSiteId())
                .filter(externalConsent -> externalConsent.getExpiryTimestamp().isAfter(Instant.now(clock)))
                .map(ExternalConsent::getExternalConsentId)
                .orElse(null);
    }

}
