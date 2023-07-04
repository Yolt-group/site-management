package nl.ing.lovebird.sitemanagement.flywheel;

import brave.Span;
import brave.Tracer;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
class InternalFlywheelService {
    private final InternalFlywheelProperties properties;
    private final UserService userService;
    private final PostgresUserSiteRepository postgresUserSiteRepository;
    private final brave.Tracer braveTracer;
    private final SiteService siteService;
    private final InternalFlywheelRefreshService internalFlywheelRefreshService;
    private final UserRefreshProperties userRefreshProperties;
    private final FlywheelUserUUIDRangeSelector flywheelUserUUIDRangeSelector;

    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    void refreshUserSitesAsync(final LocalTime nowUtc) {
        try {
            if (!properties.isEnabled()) {
                log.info("Internal flywheel is disabled. This task will not be executed.");
                return;
            }

            processRefreshesForCurrentMinute(nowUtc);
            log.info("Internal flywheel work done.");
        } catch (RuntimeException e) {
            log.error("Error in internal flywheel: {}", e.getMessage(), e);
        }
    }

    void forceRefreshUserSitesForSpecificUserAsync(UUID userId) {
        try {
            log.info("Triggering internal flywheel for user {}", userId);

            final User user = userService.getUserOrThrow(userId);

            internalFlywheelRefreshService.refreshForUser(userId, user.isOneOffAis(), true);

            log.info("Finished internal flywheel for user {}", userId);
        } catch (RuntimeException e) {
            log.error("Error in internal flywheel for user {}: {}", userId, e.getMessage(), e);
        }
    }


    @VisibleForTesting
    void processRefreshesForCurrentMinute(final LocalTime nowUtc) {
        Set<UUID> userIds = getUserIdsForCurrentMinute(nowUtc);

        log.info("Got a total of {} users to be refreshed at {} within internal flywheel", userIds.size(), nowUtc);

        userIds.forEach(userId -> {
            Span newUserSpan = this.braveTracer.newTrace();
            try (Tracer.SpanInScope ignored = this.braveTracer.withSpanInScope(newUserSpan.start());
                 LogBaggage b = LogBaggage.builder().userId(userId).build()) {

                final Optional<User> optionalUser = userService.getUser(userId);
                if (optionalUser.isEmpty()) {
                    log.info("Skipping flywheel for this user, couldn't find user anymore.");
                    return;
                }

                final User user = optionalUser.get();

                if (StatusType.BLOCKED.equals(user.getStatus())) {
                    log.info("Skipping refresh because user is blocked.");
                    return;
                }

                internalFlywheelRefreshService.refreshForUser(userId, user.isOneOffAis(), false);

            } catch (Exception e) {
                log.error("Failed to send user-sites for refresh for user {}", userId, e);
            } finally {
                newUserSpan.finish();
            }
        });
    }

    private Set<UUID> getUserIdsForCurrentMinute(LocalTime nowUtc) {

        // We need to know all clientIds, because for some clients we need to refresh once a day, and sometimes 4 times a day. So the range of user uuids that we need to refresh
        // are different per client.
        Set<ClientId> clientIdsWithAtLeastOneUserSite = postgresUserSiteRepository.getClientIdsWithUserSite();

        Map<ClientId, List<UUID>> usersPerClient = clientIdsWithAtLeastOneUserSite.stream()
                .collect(Collectors.toMap(Function.identity(), clientId -> usersToRefreshForClient(clientId, nowUtc)));

        usersPerClient.forEach((client, userIds) -> {
            log.info("Got {} users to be refreshed at {} within internal flywheel for client {}", userIds.size(), nowUtc, client); //NOSHERIFF
        });
        return usersPerClient.entrySet().stream().flatMap(it -> it.getValue().stream()).collect(Collectors.toSet());
    }


    @VisibleForTesting
    List<UUID> usersToRefreshForClient(ClientId clientId, LocalTime nowUtc) {
        final int clientRefreshesPerDay = userRefreshProperties.getRefreshesPerDay()
                .getOrDefault(clientId.unwrap(), userRefreshProperties.getDefaultRefreshesPerDay());

        if (clientRefreshesPerDay == 0) {
            return Collections.emptyList();
        }

        FlywheelUserUUIDRangeSelector.UUIDRange uuidRange = flywheelUserUUIDRangeSelector.getUUIRange(clientRefreshesPerDay, nowUtc);

        List<UUID> userIdsInRange = postgresUserSiteRepository.getUserIdsBetween(uuidRange.left(), uuidRange.right(), clientId);

        return userIdsInRange;
    }
}
