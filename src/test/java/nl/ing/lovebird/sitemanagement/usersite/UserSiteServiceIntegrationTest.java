package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistUserSites;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class UserSiteServiceIntegrationTest {

    @Autowired
    private UserSiteService userSiteService;

    @Autowired
    private PostgresUserSiteRepository userSiteRepository;

    @Autowired
    private PostgresUserSiteAuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testMarkAsDelete() {

        ClientId clientId = ClientId.random();
        UUID userId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();

        // Create 5 to-be deleted
        List<PostgresUserSite> toBeDeleted = bulkPersistUserSites(5, clientId, siteId, UUID::randomUUID, (builder, i) -> builder.userId(userId), userSite -> {
            userSiteRepository.save(userSite);
            return userSite;
        });

        // Create 1 not to-be deleted
        List<PostgresUserSite> notToBeDeleted = bulkPersistUserSites(1, clientId, siteId, UUID::randomUUID, (builder, i) -> builder.userId(userId), userSite -> {
            userSiteRepository.save(userSite);
            return userSite;
        });
        assertThat(notToBeDeleted).hasSize(1);

        // Mark 5 as deleted
        toBeDeleted
                .forEach(userSite -> userSiteService.markAsDeleted(userId, userSite.getUserSiteId()));

        // Load all from database
        List<PostgresUserSite> allUserSitesIncludingDeletedOnes = userSiteService.getAllUserSitesIncludingDeletedOnes(userId);

        // Assert 5 deleted
        List<PostgresUserSite> shouldBeDeleted = allUserSitesIncludingDeletedOnes.stream()
                .filter(PostgresUserSite::isDeleted)
                .collect(Collectors.toList());
        assertThat(shouldBeDeleted).hasSize(5);

        shouldBeDeleted.forEach(userSite -> {
            assertThat(userSite.isDeleted()).isTrue();
            assertThat(userSite.getDeletedAt()).isNotNull();
        });

        // Assert 1 not deleted
        List<PostgresUserSite> shouldNotBeDeleted = allUserSitesIncludingDeletedOnes.stream()
                .filter(not(PostgresUserSite::isDeleted))
                .collect(Collectors.toList());
        assertThat(shouldNotBeDeleted).hasSize(1);

        shouldNotBeDeleted.forEach(userSite -> {
            assertThat(userSite.isDeleted()).isFalse();
            assertThat(userSite.getDeletedAt()).isNull();
        });


        // Assert audit log (plpsql trigger)
        List<PostgresUserSiteAuditLog> auditLogs = auditLogRepository.getForUser(userId);
        assertThat(auditLogs).hasSize(5);

        List<UUID> auditLoggedUserSiteIds = auditLogs.stream()
                .map(postgresUserSiteAuditLog -> postgresUserSiteAuditLog.userSiteId)
                .collect(Collectors.toList());

        List<UUID> shouldBeDeletedUserSites = shouldBeDeleted.stream()
                .map(PostgresUserSite::getUserSiteId)
                .collect(Collectors.toList());

        assertThat(auditLoggedUserSiteIds).containsExactlyInAnyOrderElementsOf(shouldBeDeletedUserSites);

        List<Instant> auditLogDeletedAt = auditLogs.stream()
                .map(entry -> {
                    try {
                        return objectMapper.readValue(entry.metadata, Instant.class);
                    } catch (JsonProcessingException e) {
                        throw new AssertionError(e);
                    }
                })
                .collect(Collectors.toList());

        assertThat(auditLogDeletedAt)
                .allSatisfy(instant -> assertThat(Duration.between(instant, Instant.now()).toMinutes()).isLessThan(1)); // allow 1 minute skew when comparing dates
    }

    @Test
    public void testMarkUserSitesConnected() {

        ClientId clientId = ClientId.random();
        UUID userId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();

        // Create 5 disconnected user sites
        List<PostgresUserSite> disconnectedUserSites = bulkPersistUserSites(5, clientId, siteId, UUID::randomUUID, (builder, i) ->
                builder.userId(userId)
                        .connectionStatus(ConnectionStatus.DISCONNECTED)
                        .failureReason(FailureReason.TECHNICAL_ERROR), userSite -> {
            userSiteRepository.save(userSite);
            return userSite;
        });

        // Mark 2 user sites as connected
        UUID toBeConnectedUserSite1 = disconnectedUserSites.get(0).getUserSiteId();
        UUID toBeConnectedUserSite2 = disconnectedUserSites.get(3).getUserSiteId();
        userSiteService.markUserSitesConnected(userId, List.of(toBeConnectedUserSite1, toBeConnectedUserSite2));

        // Load all from database
        List<PostgresUserSite> allUserSitesIncludingDeletedOnes = userSiteService.getAllUserSitesIncludingDeletedOnes(userId);
        List<UUID> connectedUserSiteIds = allUserSitesIncludingDeletedOnes.stream()
                .filter(userSite -> userSite.getConnectionStatus().equals(ConnectionStatus.CONNECTED) && userSite.getFailureReason() == null)
                .map(PostgresUserSite::getUserSiteId)
                .toList();

        assertThat(connectedUserSiteIds).containsExactlyInAnyOrder(toBeConnectedUserSite1, toBeConnectedUserSite2);
    }
}
