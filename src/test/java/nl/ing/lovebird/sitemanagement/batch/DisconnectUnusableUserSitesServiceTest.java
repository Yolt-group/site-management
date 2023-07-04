package nl.ing.lovebird.sitemanagement.batch;

import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static nl.ing.lovebird.sitemanagement.lib.TestUtil.AIS_WITH_REDIRECT_STEPS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DisconnectUnusableUserSitesServiceTest {

    private static final String NORMAL_PROVIDER = "YOLT_PROVIDER";
    private static final String BLACKLISTED_PROVIDER = "SALTEDGE";
    private static final Site NORMAL_SITE = createRandomSite("normal", NORMAL_PROVIDER);
    private static final Site BLACKLISTED_SITE = createRandomSite("blacklisted", BLACKLISTED_PROVIDER);

    private final BatchUserSiteRepository batchUserSiteRepository = mock(BatchUserSiteRepository.class);
    private final SiteService siteService = mock(SiteService.class);
    private final SiteManagementMetrics siteManagementMetrics = mock(SiteManagementMetrics.class);

    private final DisconnectUnusableUserSitesService systemUnderTest = new DisconnectUnusableUserSitesService(siteService, batchUserSiteRepository, siteManagementMetrics);

    @Test
    public void disconnectAllUnusable_happyFlow_disconnectsExpectedSites() {
        when(siteService.getSites()).thenReturn(List.of(NORMAL_SITE));
        when(batchUserSiteRepository.disconnectUserSitesWithMigrationStatus(any(), eq(false))).thenReturn(1L);
        when(batchUserSiteRepository.disconnectUserSitesNotRefreshedFor90Days(false)).thenReturn(2L);

        final var result = systemUnderTest.disconnectAllUnusable(false);

        assertThat(result.join()).isEqualTo(3);

        verify(siteManagementMetrics, times(1)).incrementDisconnectedUserSiteNotRefreshedFor90Days(2L);

        verify(batchUserSiteRepository).disconnectUserSitesWithMigrationStatus(List.of(MigrationStatus.MIGRATING_FROM, MigrationStatus.MIGRATING_TO), false);
        verify(batchUserSiteRepository).disconnectUserSitesNotRefreshedFor90Days(false);
        verifyNoMoreInteractions(batchUserSiteRepository);
    }

    @Test
    public void disconnectAllUnusable_blacklistedSite_doesNotDisconnectSite() {
        // This job also disconnected blacklisted sites before. The job shouldn't do this! Budget Insight, for example,
        // pushes data towards us instead of that we fetch data from them. To not fetch data when the flywheel runs,
        // Budget Insight has been marked as a blacklisted site. For this reason, this job shouldn't disconnect
        // user sites of such a (blacklisted) site. Note that obsolete user sites will be disconnected anyway when
        // they have not been refreshed for 90 days.

        when(siteService.getSites()).thenReturn(List.of(BLACKLISTED_SITE));
        when(batchUserSiteRepository.disconnectUserSitesWithMigrationStatus(any(), eq(false))).thenReturn(0L);
        when(batchUserSiteRepository.disconnectUserSitesNotRefreshedFor90Days(false)).thenReturn(0L);

        final var result = systemUnderTest.disconnectAllUnusable(false);
        assertThat(result.join()).isEqualTo(0);

        verify(batchUserSiteRepository).disconnectUserSitesWithMigrationStatus(List.of(MigrationStatus.MIGRATING_FROM, MigrationStatus.MIGRATING_TO), false);
        verify(batchUserSiteRepository).disconnectUserSitesNotRefreshedFor90Days(false);
        verifyNoMoreInteractions(batchUserSiteRepository);
    }

    private static Site createRandomSite(String name, String provider, Consumer<Site> setters) {
        final var site = createRandomSite(name, provider);
        setters.accept(site);
        return site;
    }

    private static Site createRandomSite(String name, String provider) {
        return SiteCreatorUtil.createTestSite("33aca8b9-281a-4259-8492-1b37706af6db", name, provider, List.of(AccountType.values()),  List.of(CountryCode.GB), AIS_WITH_REDIRECT_STEPS);
    }
}
