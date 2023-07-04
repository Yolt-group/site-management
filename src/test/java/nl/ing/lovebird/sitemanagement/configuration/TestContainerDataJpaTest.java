package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.postgres.test.EnableExternalPostgresTestDatabase;
import nl.ing.lovebird.sitemanagement.batch.BatchUserSiteRepository;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import nl.ing.lovebird.sitemanagement.health.EventRepository;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteLockRepository;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteMaintenanceRepository;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to replace the {@link DataJpaTest} annotation.
 * <<p>
 * Rather than testing against an in memory database we test against an actual
 * postgres database, running either locally in a docker container or as a
 * service in CI. This database is provided via {@link @EnableExternalPostgresTestDatabase}
 * <p>
 * The drawback of this method is that we need to update the list of
 * {@link ContextConfiguration} for each new Repository we add to this service.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {
        PostgresUserSiteRepository.class,
        PostgresUserSiteMaintenanceRepository.class,
        BatchUserSiteRepository.class,
        PostgresEntityManagerConfiguration.class,
        PostgresUserSiteLockRepository.class,
        EventRepository.class,
        ActivityRepository.class,
        MutableTestClockConfiguration.class
})
@EnableExternalPostgresTestDatabase
public @interface TestContainerDataJpaTest {
    // Annotations only.
}
