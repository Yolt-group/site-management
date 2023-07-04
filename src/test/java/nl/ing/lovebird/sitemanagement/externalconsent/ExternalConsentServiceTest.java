package nl.ing.lovebird.sitemanagement.externalconsent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.Clock.fixed;
import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ExternalConsentServiceTest {

    static WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    private static final int CONSENT_EXPIRY_IN_DAYS = 90;


    // Dependencies
    @Mock
    private ExternalConsentRepository consentRepository;

    @Mock
    private SiteService siteService;


    private Clock clock;

    // Subject
    private ExternalConsentService subject;

    @BeforeEach
    void setUp() {
        clock = fixed(Instant.now(systemUTC()), ZoneId.of("UTC"));

        subject = new ExternalConsentService(clock, consentRepository, null, false);
    }

    @Test
    void createOrUpdateConsent_happyFlow() {

        UUID userId = UUID.randomUUID();
        Site site = SiteCreatorUtil.createTestSite(UUID.randomUUID(), "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, CONSENT_EXPIRY_IN_DAYS, null);
        UUID userSiteId = UUID.randomUUID();
        String externalConsentId = "external-consent-id";

        subject.createOrUpdateConsent(userId, site, userSiteId, externalConsentId);

        Instant nowPlusExpiry = Instant.now(clock).plus(CONSENT_EXPIRY_IN_DAYS, ChronoUnit.DAYS);
        String expiryWeek = ConsentExpiryTimeFormatter.format(nowPlusExpiry);
        ExternalConsent expectedObject = new ExternalConsent(userId, site.getId(), userSiteId, expiryWeek, nowPlusExpiry, Instant.now(clock), externalConsentId);

        verify(consentRepository).persist(expectedObject);

    }

    @Test
    void createOrUpdateConsent_persistsIfConsentIdIsNull() {

        UUID userId = UUID.randomUUID();
        Site site = SiteCreatorUtil.createTestSite(UUID.randomUUID(), "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, CONSENT_EXPIRY_IN_DAYS, null);
        UUID userSiteId = UUID.randomUUID();

        subject.createOrUpdateConsent(userId, site, userSiteId, null);

        Instant nowPlusExpiry = Instant.now(clock).plus(CONSENT_EXPIRY_IN_DAYS, ChronoUnit.DAYS);
        String expiryWeek = ConsentExpiryTimeFormatter.format(nowPlusExpiry);
        ExternalConsent expectedObject = new ExternalConsent(userId, site.getId(), userSiteId, expiryWeek, nowPlusExpiry, Instant.now(clock), null);

        verify(consentRepository).persist(expectedObject);

    }

    @Test
    void createOrUpdateConsent_configForSiteIsMissing_doesNotPersistConsent() {

        Site siteWithoutConfig = SiteCreatorUtil.createTestSite(UUID.randomUUID(), "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, null, null);

        UUID userId = UUID.randomUUID();
        UUID userSiteId = UUID.randomUUID();
        String externalConsentId = "external-consent-id";

        subject.createOrUpdateConsent(userId, siteWithoutConfig, userSiteId, externalConsentId);

        verifyNoInteractions(consentRepository);
    }

    @Test
    void findExternalConsentIdBy_nonExistingConsent_returnsEmptyOptional() {

        Optional<ExternalConsent> consentId = subject.findById(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertFalse(consentId.isPresent());
    }

    @Test
    void findExternalConsentIdBy_existingConsentWithConsentId_returnsOptional() {

        // Given
        String expectedConsentId = "consentId";
        Instant now = Instant.now(clock);
        Instant nowIn90Days = now.plus(90, ChronoUnit.DAYS);
        ExternalConsent consentWithConsentId = new ExternalConsent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ConsentExpiryTimeFormatter.format(nowIn90Days), nowIn90Days, now, expectedConsentId);

        when(consentRepository.findBy(consentWithConsentId.getUserId(), consentWithConsentId.getSiteId(), consentWithConsentId.getUserSiteId())).thenReturn(Optional.of(consentWithConsentId));

        // When
        Optional<ExternalConsent> consent = subject.findById(consentWithConsentId.getUserId(), consentWithConsentId.getSiteId(), consentWithConsentId.getUserSiteId());

        // Then
        assertThat(consent.isPresent()).isTrue();
        assertThat(consent.get().getExternalConsentId()).isEqualTo(expectedConsentId);
    }

    @Test
    void shouldGetConsentFromStubs_whenUsingYoltBankOpenBankingStubbed() {
        // Given
        final UUID userId = UUID.randomUUID();
        Site site = SiteCreatorUtil.createTestSite(SiteService.ID_YOLTBANK_OPENBANKING_STUBBED, "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, CONSENT_EXPIRY_IN_DAYS, null);


        final UUID userSiteId = UUID.randomUUID();
        final String externalConsentId = "external-consent-id";

        wireMockServer.stubFor(get(String.format("/stubs/site-consent-expiries/users/%s/sites/%s", userId, SiteService.ID_YOLTBANK_OPENBANKING_STUBBED))
                .willReturn(okJson("{\"expiryInDays\": 100}")));

        // When
        subject = new ExternalConsentService(clock, consentRepository, new ConsentExpiryStub(new ObjectMapper(), "http://localhost:" + wireMockServer.port()), true);

        // Then
        subject.createOrUpdateConsent(userId, site, userSiteId, externalConsentId);

        Instant nowPlusExpiry = Instant.now(clock).plus(100, ChronoUnit.DAYS);
        String expiryWeek = ConsentExpiryTimeFormatter.format(nowPlusExpiry);
        ExternalConsent expectedObject = new ExternalConsent(userId, site.getId(), userSiteId, expiryWeek, nowPlusExpiry, Instant.now(clock), externalConsentId);

        verify(consentRepository).persist(expectedObject);
    }

    @Test
    void shouldNotGetConsentFromStubs_whenNotUsingYoltBankOpenBankingStubbed() {
        // Given
        final UUID userId = UUID.randomUUID();
        final UUID userSiteId = UUID.randomUUID();
        final String externalConsentId = "external-consent-id";
        Site site = SiteCreatorUtil.createTestSite(SiteService.ID_YOLTBANK_YOLT_PROVIDER, "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, CONSENT_EXPIRY_IN_DAYS, null);
        // When
        subject = new ExternalConsentService(clock, consentRepository, new ConsentExpiryStub(new ObjectMapper(), "http://localhost:" + wireMockServer.port()), true);

        // Then
        subject.createOrUpdateConsent(userId, site, userSiteId, externalConsentId);

        Instant nowPlusExpiry = Instant.now(clock).plus(CONSENT_EXPIRY_IN_DAYS, ChronoUnit.DAYS);
        String expiryWeek = ConsentExpiryTimeFormatter.format(nowPlusExpiry);
        ExternalConsent expectedObject = new ExternalConsent(userId, site.getId(), userSiteId, expiryWeek, nowPlusExpiry, Instant.now(clock), externalConsentId);

        verify(consentRepository).persist(expectedObject);
        wireMockServer.verify(0, anyRequestedFor(anyUrl()));
    }

}