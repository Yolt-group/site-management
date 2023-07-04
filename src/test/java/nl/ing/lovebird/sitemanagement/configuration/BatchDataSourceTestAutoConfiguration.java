package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.postgres.autoconfigure.YoltPostgreSqlAutoConfiguration;
import nl.ing.lovebird.sitemanagement.batch.BatchQueryEntityManagerConfiguration;
import nl.ing.lovebird.sitemanagement.batch.PeriodicBatchController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 * Autoconfiguration for the additional datasource.
 * We create an additional {@link DataSource} and {@link EntityManager} for time-intensive queries in our {@link BatchQueryEntityManagerConfiguration}.
 * However, these do not work well in our test setup so we alias the {@link DataSource} that is already on our spring
 * context to piggyback off the configuration that is already done for us. These batch queries only run in deployed
 * environments so there is no added value of using the custom beans in our tests.
 */
@Configuration
@AutoConfigureAfter(YoltPostgreSqlAutoConfiguration.class)
public class BatchDataSourceTestAutoConfiguration {

    @Bean(name = "batchDataSource")
    public DataSource batchDataSource(@Qualifier("dataSource") final DataSource dataSource) {
        return dataSource;
    }

    @Bean(name = "batchEntityManager")
    public LocalContainerEntityManagerFactoryBean batchEntityManagerFactory(final EntityManagerFactoryBuilder entityManagerFactoryBuilder,
                                                                            @Qualifier("batchDataSource") final DataSource batchDataSource) {
        return entityManagerFactoryBuilder
                .dataSource(batchDataSource)
                .persistenceUnit("batchEntityManager")
                .packages(PeriodicBatchController.class)
                .build();
    }

    @Bean(name = "batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(final LocalContainerEntityManagerFactoryBean batchEntityManagerFactory) {
        return new JpaTransactionManager(batchEntityManagerFactory.getObject());
    }
}
