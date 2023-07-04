package nl.ing.lovebird.sitemanagement.login;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsService;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.LoginFormService;
import nl.ing.lovebird.sitemanagement.usersite.SiteLoginService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class SiteLoginServiceTest {

    private final UUID USER_ID = UUID.randomUUID();

    private final ClientId CLIENT_ID = ClientId.random();
    private final UUID CLIENT_GROUP_ID = UUID.randomUUID();

    @Mock
    private LoginFormService loginFormService;
    @Mock
    private EncryptionDetailsService encryptionDetailsService;
    @Mock
    private ClientRedirectUrlService clientRedirectUrlService;
    @Mock
    private ConsentSessionService userSiteSessionService;
    @Mock
    private ClientSiteService clientSiteService;
    @Mock
    private ProviderRestClient providerRestClient;
    @Mock
    private ClientToken clientToken;
    @Mock
    private SiteManagementMetrics siteManagementMetrics;
    @Mock
    private AuthenticationMeansFactory authenticationMeansFactory;
    @Mock
    private SitesProvider sitesProvider;

    private SiteLoginService siteLoginService;

    @BeforeEach
    void setUp() {
        siteLoginService = new SiteLoginService(
                clientRedirectUrlService,
                loginFormService,
                userSiteSessionService,
                clientSiteService,
                providerRestClient,
                encryptionDetailsService,
                siteManagementMetrics,
                authenticationMeansFactory,
                sitesProvider
        );

        when(clientToken.getClientIdClaim()).thenReturn(CLIENT_ID.unwrap());
        when(clientToken.getClientGroupIdClaim()).thenReturn(CLIENT_GROUP_ID);

        Site testSite = SiteCreatorUtil.createTestSite("33aca8b9-281a-4259-8492-1b37706af6db",
                "YoltProvider",
                "YOLT_PROVIDER",
                List.of(AccountType.values()),
                List.of(CountryCode.GB),
                Map.of(ServiceType.AIS, Collections.singletonList(LoginRequirement.REDIRECT))
        );
        when(sitesProvider.findByIdOrThrow(UUID.fromString("33aca8b9-281a-4259-8492-1b37706af6db"))).thenReturn(testSite);

    }


}
