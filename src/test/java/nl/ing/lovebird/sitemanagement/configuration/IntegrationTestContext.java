package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.cassandra.test.EnableExternalCassandraTestDatabase;
import nl.ing.lovebird.kafka.test.EnableExternalKafkaTestCluster;
import nl.ing.lovebird.postgres.test.EnableExternalPostgresTestDatabase;
import nl.ing.lovebird.sitemanagement.SiteManagementApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = {SiteManagementApplication.class, MutableTestClockConfiguration.class, TestConfiguration.class}, webEnvironment = RANDOM_PORT)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable transactions in test scope because of MVCC visibility
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@TestPropertySource(properties = {"management.port=0"})
@EnableExternalCassandraTestDatabase
@EnableExternalPostgresTestDatabase
@EnableExternalKafkaTestCluster
public @interface IntegrationTestContext {
    // Annotations only
}
