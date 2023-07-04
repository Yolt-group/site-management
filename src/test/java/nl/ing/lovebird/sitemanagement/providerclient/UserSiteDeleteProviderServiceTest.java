package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.NonNull;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeans;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserSiteDeleteProviderServiceTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final UUID REDIRECT_URL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final UUID SITE_ID = UUID.randomUUID();
    private static final String EXTERNAL_ID = "325234";
    private static final PostgresUserSite SCRAPER_USER_SITE;

    static {
        @NonNull @NotNull Date created = new Date();
        Date deletedAt = Date.from(Instant.now());
        SCRAPER_USER_SITE = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, EXTERNAL_ID, ConnectionStatus.CONNECTED, null, null, created != null ? created.toInstant() : null, null, null, CLIENT_ID, "YODLEE", null, REDIRECT_URL_ID, null, false, deletedAt != null ? deletedAt.toInstant() : null);
    }

    private static final PostgresUserSite DIRECT_CONNECTION_USER_SITE;

    static {
        @NonNull @NotNull Date created = new Date();
        Date deletedAt = Date.from(Instant.now());
        DIRECT_CONNECTION_USER_SITE = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, EXTERNAL_ID, ConnectionStatus.CONNECTED, null, null, created != null ? created.toInstant() : null, null, null, CLIENT_ID, "YOLT_PROVIDER", null, REDIRECT_URL_ID, null, false, deletedAt != null ? deletedAt.toInstant() : null);
    }

    private static final AccessMeans ACCESS_MEANS = new AccessMeans(USER_ID, "YODLEE", "secret-access-means", new Date(), new Date());
    private static final FormDeleteUserSiteDTO FORM_DELETE_USER_SITE = new FormDeleteUserSiteDTO(ACCESS_MEANS.getAccessMeans(), EXTERNAL_ID, USER_ID, CLIENT_ID);
    private static final ClientUserToken CLIENT_TOKEN = mock(ClientUserToken.class);

    @Mock
    private FormProviderRestClient formProviderRestClient;

    @Mock
    private ProviderRestClient providerRestClient;

    @Mock
    private ExternalConsentService consentService;

    @Mock
    private SiteService siteService;

    @InjectMocks
    private UserSiteDeleteProviderService userSiteDeleteProviderService;

    @BeforeEach
    void setUp() {
        Site testSite = SiteCreatorUtil.createTestSite(SITE_ID.toString(), "someSite", "STARLINGBANK", List.of(), List.of(), Map.of());
        when(siteService.getSite(SITE_ID)).thenReturn(testSite);
    }

    @Test
    @SneakyThrows
    public void doesNotDeleteFormUserSiteWithTwoEqualExternalIds() {
        PostgresUserSite userSite2 = new PostgresUserSite(USER_ID, UUID.randomUUID(), SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null);
        userSite2.setExternalId(EXTERNAL_ID);
        List<PostgresUserSite> otherUserSitesFromSameProvider = singletonList(userSite2);

        userSiteDeleteProviderService.deleteUserSiteBlocking(SCRAPER_USER_SITE, otherUserSitesFromSameProvider, null, CLIENT_TOKEN);

        verify(formProviderRestClient, never()).deleteUserSite("YODLEE", FORM_DELETE_USER_SITE, CLIENT_TOKEN, SITE_ID);
        verifyNoInteractions(consentService);
    }

    @Test
    @SneakyThrows
    public void deleteUserSiteBlocking_form_WithEmptyExternalId() {
        PostgresUserSite userSite = new PostgresUserSite(USER_ID, UUID.randomUUID(), SITE_ID, null, ConnectionStatus.DISCONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null);

        userSiteDeleteProviderService.deleteUserSiteBlocking(userSite, singletonList(userSite), null, null);

        verify(formProviderRestClient, never()).deleteUserSite(any(String.class), any(FormDeleteUserSiteDTO.class), any(), any(UUID.class));
        verifyNoInteractions(consentService);
    }

    @Test
    void testDeleteUrlUserSiteBlocking_noExternalConsentPresent() {

        userSiteDeleteProviderService.deleteUserSiteBlocking(DIRECT_CONNECTION_USER_SITE, emptyList(), null, null);

        verifyNoInteractions(formProviderRestClient);
        verify(consentService, never()).deleteForUserSite(DIRECT_CONNECTION_USER_SITE.getUserId(), DIRECT_CONNECTION_USER_SITE.getSiteId(), DIRECT_CONNECTION_USER_SITE.getUserSiteId());
    }
    
}
