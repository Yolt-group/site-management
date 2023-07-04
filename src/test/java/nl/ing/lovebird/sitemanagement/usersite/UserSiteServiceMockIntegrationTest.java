package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.lib.ClientIds;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserSiteServiceMockIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SITE_ID = UUID.randomUUID();
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final ClientId CLIENT_ID = ClientIds.YTS_CREDIT_SCORING_APP;

    private final List<PostgresUserSite> mockedUserSitesInRepository = new ArrayList<>();

    @Mock
    private PostgresUserSiteLockRepository userSiteLockRepository;
    @Mock
    private PostgresUserSiteAuditLogRepository postgresUserSiteAuditLogRepository;
    @Mock
    private LastFetchedService lastFetchedService;
    @Mock
    private UserSiteEventService userSiteEventService;
    @Mock
    private PostgresUserSiteRepository postgresUserSiteRepository;
    @Mock
    private SiteManagementMetrics siteManagementMetrics;
    @Mock
    private SitesProvider sitesProvider;

    private UserSiteService userSiteService;

    @Mock
    private SiteService siteService;

    @BeforeEach
    void setUp() {
        userSiteService = new UserSiteService(systemUTC(), postgresUserSiteRepository,
                userSiteLockRepository, userSiteEventService, null, siteManagementMetrics, sitesProvider);

        when(postgresUserSiteRepository.getUserSite(any(UUID.class), any(UUID.class))).thenAnswer(invocationOnMock -> {
            UUID userId = (UUID) invocationOnMock.getArguments()[0];
            UUID userSiteId = (UUID) invocationOnMock.getArguments()[1];
            return mockedUserSitesInRepository.stream()
                    .filter(userSite -> userSite.getUserId().equals(userId) && userSite.getUserSiteId().equals(userSiteId))
                    .findFirst();
        });
        when(postgresUserSiteRepository.getUserSites(any(UUID.class))).thenAnswer(invocationOnMock -> {
            UUID userId = (UUID) invocationOnMock.getArguments()[0];
            return mockedUserSitesInRepository.stream()
                    .filter(userSite -> userSite.getUserId().equals(userId))
                    .collect(Collectors.toList());
        });
    }

    @Test
    void testGetUserSitesWithNeededActions() {

        final PostgresUserSite userSite = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, null, new Date().toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null);
        mockedUserSitesInRepository.add(userSite);
        final PostgresUserSite userSite2 = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "YODLEE", null, null, null, false, null);
        mockedUserSitesInRepository.add(userSite2);

        List<PostgresUserSite> userSites = userSiteService.getNonDeletedUserSites(USER_ID);

        assertThat(userSites.size()).isEqualTo(2);
        assertThat(userSites.stream().map(PostgresUserSite::determineUserSiteNeededAction)
                .collect(Collectors.toList())).containsExactlyInAnyOrder(null, UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void testGetExistingUserSiteWithDefaultNullProvider() {
        final PostgresUserSite userSite1 = new PostgresUserSite(USER_ID, USER_SITE_ID, SITE_ID, null, ConnectionStatus.CONNECTED, null, null, new Date().toInstant(), null, null, CLIENT_ID, "MONZO", null, null, null, false, null);
        // Can only do this with the default constructor which we shouldn't use in code. It's there for datastax. There might still be records
        // in the database with this field being null.
        ReflectionTestUtils.setField(userSite1, "provider", null);
        mockedUserSitesInRepository.add(userSite1);

        final PostgresUserSite existingUserSite = userSiteService.getUserSite(USER_ID, USER_SITE_ID);

        assertThat(existingUserSite.getUserSiteId()).isEqualTo(USER_SITE_ID);
    }

    @Test
    void testGetExistingUserSite_NotFound() {
        assertThatThrownBy(() -> {
            when(postgresUserSiteRepository.getUserSite(USER_ID, USER_SITE_ID)).thenReturn(Optional.empty());
            userSiteService.getUserSite(USER_ID, USER_SITE_ID);
        }).isInstanceOf(UserSiteNotFoundException.class);
    }
}
