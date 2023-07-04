package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.sitemanagement.batch.BatchQueryEntityManagerConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 * This is the default {@link EntityManager}.
 * If you don't explicitly configure the {@link BatchQueryEntityManagerConfiguration#batchEntityManagerFactory}
 * you will end up with this entitymanager.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "nl.ing.lovebird.sitemanagement",
        entityManagerFactoryRef = "entityManager")
public class PostgresEntityManagerConfiguration {

    @Primary
    @Bean(name = "entityManager")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final EntityManagerFactoryBuilder entityManagerFactoryBuilder,
                                                                              @Qualifier("dataSource") final DataSource dataSource) {
        return entityManagerFactoryBuilder
                .dataSource(dataSource)
                .persistenceUnit("entityManager")
                .packages("nl.ing.lovebird.sitemanagement")
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(final LocalContainerEntityManagerFactoryBean entityManager) {
        return new JpaTransactionManager(entityManager.getObject());
    }
}
