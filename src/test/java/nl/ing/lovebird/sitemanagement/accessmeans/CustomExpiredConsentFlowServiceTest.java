package nl.ing.lovebird.sitemanagement.accessmeans;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomExpiredConsentFlowServiceTest {

    @Mock
    private UserSiteAccessMeansRepository userSiteAccessMeansRepository;

    @Mock
    private CustomExpiredConsentFlowConfiguration customExpiredConsentFlowConfiguration;

    private CustomExpiredConsentFlowService customExpiredConsentFlowService;

    @BeforeEach
    void beforeEach() {
        customExpiredConsentFlowService = new CustomExpiredConsentFlowService(Clock.systemUTC(), customExpiredConsentFlowConfiguration, userSiteAccessMeansRepository);
    }

    @Test
    void shouldDisconnectOnConsentExpired_notApplicableClientAndUserSite_returnsTrue() {
        var userSite = new PostgresUserSite();
        userSite.setClientId(ClientId.random());
        userSite.setSiteId(randomUUID());

        when(customExpiredConsentFlowConfiguration.appliesToClientAndSite(userSite.getClientId(), userSite.getSiteId())).thenReturn(false);

        assertThat(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).isTrue();

        verify(userSiteAccessMeansRepository, never()).get(any(), any(), any());
    }

    @Test
    void shouldDisconnectOnConsentExpired_applicableClientAndUserSiteButConsentYoungerThanThreshold_returnsFalse() {
        var userSite = new PostgresUserSite();
        userSite.setClientId(ClientId.random());
        userSite.setSiteId(randomUUID());
        userSite.setUserId(randomUUID());
        userSite.setUserSiteId(randomUUID());
        userSite.setProvider("SomeProvider");

        var userSiteAccessMeans = new UserSiteAccessMeans();
        userSiteAccessMeans.setCreated(Instant.now().minus(89, ChronoUnit.DAYS));

        when(customExpiredConsentFlowConfiguration.appliesToClientAndSite(userSite.getClientId(), userSite.getSiteId())).thenReturn(true);
        when(customExpiredConsentFlowConfiguration.getMinimumConsentAgeBeforeDisconnect()).thenReturn(Duration.ofDays(90));
        when(userSiteAccessMeansRepository.get(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider())).thenReturn(Optional.of(userSiteAccessMeans));

        assertThat(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).isFalse();
    }

    @Test
    void shouldDisconnectOnConsentExpired_applicableClientAndUserSiteAndConsentOlderThanThreshold_returnsTrue() {
        var userSite = new PostgresUserSite();
        userSite.setClientId(ClientId.random());
        userSite.setSiteId(randomUUID());
        userSite.setUserId(randomUUID());
        userSite.setUserSiteId(randomUUID());
        userSite.setProvider("SomeProvider");

        var userSiteAccessMeans = new UserSiteAccessMeans();
        userSiteAccessMeans.setCreated(Instant.now().minus(90, ChronoUnit.DAYS).minus(10, ChronoUnit.SECONDS));

        when(customExpiredConsentFlowConfiguration.appliesToClientAndSite(userSite.getClientId(), userSite.getSiteId())).thenReturn(true);
        when(customExpiredConsentFlowConfiguration.getMinimumConsentAgeBeforeDisconnect()).thenReturn(Duration.ofDays(90));
        when(userSiteAccessMeansRepository.get(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider())).thenReturn(Optional.of(userSiteAccessMeans));

        assertThat(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).isTrue();
    }

    @Test
    void shouldDisconnectOnConsentExpired_applicableClientAndUserSiteButBankConsentNotFound_returnsTrue() {
        var userSite = new PostgresUserSite();
        userSite.setClientId(ClientId.random());
        userSite.setSiteId(randomUUID());
        userSite.setUserId(randomUUID());
        userSite.setUserSiteId(randomUUID());
        userSite.setProvider("SomeProvider");

        var userSiteAccessMeans = new UserSiteAccessMeans();
        userSiteAccessMeans.setCreated(Instant.now().minus(90, ChronoUnit.DAYS).minus(10, ChronoUnit.SECONDS));

        when(customExpiredConsentFlowConfiguration.appliesToClientAndSite(userSite.getClientId(), userSite.getSiteId())).thenReturn(true);
        when(userSiteAccessMeansRepository.get(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider())).thenReturn(Optional.empty());

        assertThat(customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite)).isTrue();
    }
}
