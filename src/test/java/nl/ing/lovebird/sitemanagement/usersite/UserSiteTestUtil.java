package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Clock.systemUTC;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;

public class UserSiteTestUtil {

    public static PostgresUserSite createRandomUserSite(final ClientId clientId, final UUID siteId, final UUID userId, final Clock clock) {
        return PostgresUserSite.builder()
                .userSiteId(randomUUID())
                .userId(userId)
                .siteId(siteId)
                .externalId("external-id")
                .connectionStatus(ConnectionStatus.CONNECTED)
                .failureReason(null)
                .statusTimeoutTime(null)
                .created(clock.instant().truncatedTo(ChronoUnit.MICROS))
                .updated(clock.instant().truncatedTo(ChronoUnit.MICROS))
                .lastDataFetch(clock.instant().truncatedTo(ChronoUnit.MICROS))
                .clientId(clientId)
                .provider("STARLINGBANK")
                .migrationStatus(MigrationStatus.NONE)
                .redirectUrlId(randomUUID())
                .persistedFormStepAnswers(singletonMap("KEY", "VALUE"))
                .build();
    }

    public static PostgresUserSite createRandomUserSite(final ClientId clientId, final UUID siteId, final UUID userId) {
        return createRandomUserSite(clientId, siteId, userId, Clock.systemUTC());
    }

    public static PostgresUserSite createRandomPostgresUserSite(final ClientId clientId, final UUID siteId, final UUID userId) {
        return PostgresUserSite.builder()
                .userSiteId(randomUUID())
                .userId(userId)
                .siteId(siteId)
                .externalId("external-id")
                .connectionStatus(ConnectionStatus.CONNECTED)
                .failureReason(null)
                .statusTimeoutTime(null)
                .created(Instant.now(systemUTC()))
                .updated(Instant.now(systemUTC()))
                .lastDataFetch(Instant.now(systemUTC()))
                .clientId(clientId)
                .provider("STARLINGBANK")
                .migrationStatus(MigrationStatus.NONE)
                .redirectUrlId(randomUUID())
                .persistedFormStepAnswers(singletonMap("KEY", "VALUE"))
                .build();
    }

    public static List<PostgresUserSite> bulkPersistPostgresUserSites(final int max,
                                                                      final ClientId clientId,
                                                                      final UUID siteId,
                                                                      final Supplier<UUID> userIdSupplier,
                                                                      final BiFunction<PostgresUserSite.PostgresUserSiteBuilder, Integer, PostgresUserSite.PostgresUserSiteBuilder> customizer,
                                                                      final Function<PostgresUserSite, PostgresUserSite> saveFunction) {
        return IntStream.range(0, max)
                .mapToObj(i -> customizer.apply(createRandomPostgresUserSite(clientId, siteId, userIdSupplier.get()).toBuilder(), i).build())
                .map(saveFunction)
                .collect(Collectors.toList());
    }

    public static List<PostgresUserSite> bulkPersistUserSites(final int max,
                                                              final ClientId clientId,
                                                              final UUID siteId,
                                                              final Supplier<UUID> userIdSupplier,
                                                              final BiFunction<PostgresUserSite.PostgresUserSiteBuilder, Integer, PostgresUserSite.PostgresUserSiteBuilder> customizer,
                                                              final Function<PostgresUserSite, PostgresUserSite> saveFunction) {
        return bulkPersistUserSites(max, clientId, siteId, userIdSupplier, customizer, saveFunction, Clock.systemUTC());
    }

    public static List<PostgresUserSite> bulkPersistUserSites(final int max,
                                                              final ClientId clientId,
                                                              final UUID siteId,
                                                              final Supplier<UUID> userIdSupplier,
                                                              final BiFunction<PostgresUserSite.PostgresUserSiteBuilder, Integer, PostgresUserSite.PostgresUserSiteBuilder> customizer,
                                                              final Function<PostgresUserSite, PostgresUserSite> saveFunction,
                                                              final Clock clock) {
        return IntStream.range(0, max)
                .mapToObj(i -> customizer.apply(createRandomUserSite(clientId, siteId, userIdSupplier.get(), clock).toBuilder(), i).build())
                .map(saveFunction)
                .collect(Collectors.toList());
    }
}
