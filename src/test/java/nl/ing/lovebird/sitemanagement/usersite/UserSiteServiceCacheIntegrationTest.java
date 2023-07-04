package nl.ing.lovebird.sitemanagement.usersite;

import com.github.tomakehurst.wiremock.WireMockServer;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.configuration.TestConfiguration.InspectableCache;
import nl.ing.lovebird.sitemanagement.exception.SiteNotFoundException;
import nl.ing.lovebird.sitemanagement.flows.lib.TestProviderSites;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.WiremockStubManager;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService.UserSiteStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteService.GeneralizedConnectionStatus.*;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.createRandomUserSite;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class UserSiteServiceCacheIntegrationTest {

    private static final UUID ABN_AMRO_ID = TestProviderSites.ABN_AMRO.getId();

    @Autowired
    private Clock clock;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private UserService userService;
    @Autowired
    private UserSiteService userSiteService;
    @Autowired
    private SitesProvider sitesProvider;
    @Autowired
    private WireMockServer wireMockServer;

    private InspectableCache cache;

    @BeforeEach
    public void onBefore() {
        this.cache = requireNonNull((InspectableCache) cacheManager.getCache("siteManagementUserSiteCache"));
        this.cache.clear();

        FauxProvidersService.setupProviderSitesStub(wireMockServer, new ProvidersSites(
                        List.of(TestProviderSites.ABN_AMRO),
                        Collections.emptyList()
                )
        );
        sitesProvider.update();
    }

    @AfterEach
    public void cleanup() {
        WiremockStubManager.clearFlowStubs(wireMockServer);
    }

    @Test
    void shouldReturnStatisticsWithCache() {
        UUID userId = UUID.randomUUID();
        ClientId clientId = ClientId.random();
        Site site = getSite(ABN_AMRO_ID.toString()); // ABN

        userService.saveUser(new User(userId, clock.instant(), clientId, StatusType.ACTIVE, false));

        IntStream.range(0, 31)
                .mapToObj(i -> createRandomUserSite(clientId, site.getId(), userId).toBuilder()
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .build())
                .forEach(userSite -> userSiteService.createNew(userSite));

        // assert non cached
        List<UserSiteStatistics> userSiteStatistics = userSiteService.getUserSiteStatistics(clientId);
        assertThat(userSiteStatistics).hasSize(1);
        assertThat(userSiteStatistics).usingElementComparatorIgnoringFields("compiledAt").containsExactly(
                UserSiteStatistics.builder()
                        .siteId(site.getId())
                        .siteName("ABN AMRO")
                        .nrOfUniqueUsers(1)
                        .nrOfUniqueConnections(31)
                        .connectionStatuses(Map.of(ACTIVE, 31, ERROR, 0, UNABLE_TO_LOGIN, 0, OTHER, 0))
                        .compiledAt(ZonedDateTime.now(clock))
                        .build());

        assertThat(cache.getGetOperations()).isEqualTo(1); // get
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent

        // assert cached
        List<UserSiteStatistics> cachedUsersiteStatistics = userSiteService.getUserSiteStatistics(clientId);
        assertThat(cachedUsersiteStatistics).hasSize(1);
        assertThat(cachedUsersiteStatistics).usingElementComparatorIgnoringFields("compiledAt").containsExactly(
                UserSiteStatistics.builder()
                        .siteId(site.getId())
                        .siteName("ABN AMRO")
                        .nrOfUniqueUsers(1)
                        .nrOfUniqueConnections(31)
                        .connectionStatuses(Map.of(ACTIVE, 31, ERROR, 0, UNABLE_TO_LOGIN, 0, OTHER, 0))
                        .compiledAt(ZonedDateTime.now(clock))
                        .build());

        assertThat(cache.getGetOperations()).isEqualTo(2); // get (increased by one)
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent
    }

    @Test
    void testCacheNotFound() {
        UUID userId = UUID.randomUUID();
        ClientId clientId = ClientId.random();
        Site site = getSite(ABN_AMRO_ID.toString()); // ABN

        userService.saveUser(new User(userId, clock.instant(), clientId, StatusType.ACTIVE, false));

        IntStream.range(0, 31)
                .mapToObj(i -> createRandomUserSite(clientId, site.getId(), userId).toBuilder()
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .build())
                .forEach(userSite -> userSiteService.createNew(userSite));

        // existing. 1 get + 1 putIfAbsent
        List<UserSiteStatistics> userSiteStatistics = userSiteService.getUserSiteStatistics(clientId);
        assertThat(userSiteStatistics).hasSize(1);

        assertThat(cache.getGetOperations()).isEqualTo(1); // get
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent

        // non existing. 1 get + 0 putIfAbsent
        List<UserSiteStatistics> u1 = userSiteService.getUserSiteStatistics(ClientId.random());
        assertThat(u1).hasSize(0);

        assertThat(cache.getGetOperations()).isEqualTo(2); // get (increased by one)
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent

        // non existing. 1 get + 0 putIfAbsent
        List<UserSiteStatistics> u2 = userSiteService.getUserSiteStatistics(ClientId.random());
        assertThat(u2).hasSize(0);

        assertThat(cache.getGetOperations()).isEqualTo(3); // get (increased by one)
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent
    }

    @Test
    void testClearCache() {
        UUID userId = UUID.randomUUID();
        ClientId clientId = ClientId.random();
        Site site = getSite(ABN_AMRO_ID.toString()); // ABN

        userService.saveUser(new User(userId, clock.instant(), clientId, StatusType.ACTIVE, false));

        IntStream.range(0, 31)
                .mapToObj(i -> createRandomUserSite(clientId, site.getId(), userId).toBuilder()
                        .lastDataFetch(clock.instant().minus(i, ChronoUnit.DAYS))
                        .build())
                .forEach(userSite -> userSiteService.createNew(userSite));

        // existing. 1 get + 1 putIfAbsent
        List<UserSiteStatistics> u1 = userSiteService.getUserSiteStatistics(clientId);
        assertThat(u1).hasSize(1);

        assertThat(cache.getGetOperations()).isEqualTo(1); // get
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent

        // evict cache.
        userSiteService.cacheEvict();

        // assert that cache got evicted by checking the get/put operations on the cache
        List<UserSiteStatistics> u2 = userSiteService.getUserSiteStatistics(clientId);
        assertThat(u2).hasSize(1);

        assertThat(cache.getGetOperations()).isEqualTo(1); // get
        assertThat(cache.getPutOperations()).isEqualTo(1); // putIfAbsent
    }

    private Site getSite(final String id) {
        return sitesProvider.allSites()
                .stream()
                .filter(registeredSite -> UUID.fromString(id).equals(registeredSite.getId()))
                .findFirst()
                .orElseThrow(() -> new SiteNotFoundException("Site with id " + id + " does not exist."));
    }
}
