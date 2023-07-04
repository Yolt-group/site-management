package nl.ing.lovebird.sitemanagement.batch;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.postgres.autoconfigure.AuthAwareDataSource;
import nl.ing.lovebird.postgres.autoconfigure.DataSourceConfigurer;
import nl.ing.lovebird.postgres.autoconfigure.YoltPostgreSqlAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.Collection;

/**
 * Site-management has a number of queries that run longer than the default configured postgresql socket timeout.
 * To allow us to run slow queries we have a separate "batch" {@link EntityManager}.
 * This entity manager uses the "batch" {@link DataSource} that is configured through {@link YoltPostgreSqlAutoConfiguration}
 * We can further configure this datasource through the `yolt.datasource.hikari.batch` properties; these properties
 * use the same bindings as a {@link HikariDataSource}, which at the time of this writing is the default datasource
 * for spring.
 * <p>
 * To use this entitymanager in your {@link Repository}, you have to add entity or package path to the entitymanager
 * factory builder.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "yolt.datasource.hikari.batch.enabled", havingValue = "true")
@EnableJpaRepositories(
        basePackageClasses = {PeriodicBatchController.class},
        entityManagerFactoryRef = "batchEntityManagerFactory")
public class BatchQueryEntityManagerConfiguration {

    @Bean(name = {"batchDataSource"})
    @ConfigurationProperties(prefix = "yolt.datasource.hikari.batch")
    @ConditionalOnMissingBean(name = {"batchDataSource"})
    public DataSource batchDataSource(final DataSourceProperties properties,
                                      final Collection<DataSourceConfigurer> dataSourceConfigurers) {
        AuthAwareDataSource dataSource = properties.initializeDataSourceBuilder().type(AuthAwareDataSource.class).build();
        dataSourceConfigurers.forEach((dataSourceConfigurer) -> dataSourceConfigurer.configure(dataSource));
        return dataSource;
    }

    @Bean(name = "batchEntityManager")
    public LocalContainerEntityManagerFactoryBean batchEntityManagerFactory(final EntityManagerFactoryBuilder entityManagerFactoryBuilder,
                                                                            @Qualifier("batchDataSource") final DataSource batchDataSource) {

        log.debug("Created a batch entity manager which can be used for long running queries. Poolname = {}", ((HikariDataSource) batchDataSource).getPoolName());
        return entityManagerFactoryBuilder
                .dataSource(batchDataSource)
                .persistenceUnit("batchEntityManager")
                // We explicitly use the package path as a parameter because it is required as a hint for spring to
                // autowire the correct entitymanager. We do not have any existing entities for the batch repository
                // so we cannot use an object as the parameter to this builder method.
                .packages(BatchUserSiteRepository.class)
                .build();
    }

    @Bean(name = "batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager(final LocalContainerEntityManagerFactoryBean batchEntityManagerFactory) {
        return new JpaTransactionManager(batchEntityManagerFactory.getObject());
    }
}
