package br.com.rafaelvieira.taskmanagement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Classe de configuração do banco de dados Configura o DataSource, EntityManagerFactory e
 * TransactionManager
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-04
 */
@Configuration
@Profile("!test")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "br.com.rafaelvieira.taskmanagement.repository")
@EntityScan(basePackages = "br.com.rafaelvieira.taskmanagement.domain.model")
@EnableJpaAuditing
@NoArgsConstructor
public class DatabaseConfig {
    private static final String TEST_PROFILE = "!test";
    private static final Integer POOL_SIZE = 20;
    private static final Integer CONNECTION_TIMEOUT = 30000;
    private static final long IDLE_TIMEOUT = 30_0000L;
    private static final Integer MIN_IDLE = 5;
    private static final long MAX_LIFETIME = 18_00000L;
    private static final String CONNECTION_TEST_QUERY = "SELECT 1";
    private static final String POOL_NAME = "TaskManagementHikariPool";
    private static final long LEAK_DETECTION_THRESHOLD = 60000L;
    private static final String SECOND_CACHE_LEVEL = "false";
    private static final String USE_QUERY_CACHE = "false";
    private static final String GENERATE_STATISTICS = "false";
    private static final String DISABLED_AUTO_COMMITS = "false";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:true}")
    private boolean formatSql;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.PostgreSQLDialect}")
    private String hibernateDialect;

    @Bean
    @Profile(TEST_PROFILE)
    public DataSource dataSource() {
        var hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUsername);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setMaximumPoolSize(POOL_SIZE);
        hikariConfig.setMinimumIdle(MIN_IDLE);
        hikariConfig.setIdleTimeout(IDLE_TIMEOUT);
        hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        hikariConfig.setMaxLifetime(MAX_LIFETIME);
        hikariConfig.setAutoCommit(true);
        hikariConfig.setConnectionTestQuery(CONNECTION_TEST_QUERY);
        hikariConfig.setPoolName(POOL_NAME);
        hikariConfig.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        return new HikariDataSource(hikariConfig);
    }

    @Bean
    @Profile(TEST_PROFILE)
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("br.com.rafaelvieira.taskmanagement.domain.model");

        var vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        vendorAdapter.setGenerateDdl(true);
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaProperties(hibernateProperties());

        return em;
    }

    private Properties hibernateProperties() {
        var properties = new Properties();
        properties.setProperty("hibernate.dialect", hibernateDialect);
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.cache.use_second_level_cache", SECOND_CACHE_LEVEL);
        properties.setProperty("hibernate.cache.use_query_cache", USE_QUERY_CACHE);
        properties.setProperty("hibernate.generate_statistics", GENERATE_STATISTICS);
        properties.setProperty(
                "hibernate.connection.provider_disables_autocommit", DISABLED_AUTO_COMMITS);
        properties.setProperty("hibernate.bytecode.provider", "bytebuddy");

        return properties;
    }

    @Bean
    @Profile(TEST_PROFILE)
    public PlatformTransactionManager transactionManager(
            AbstractEntityManagerFactoryBean entityManagerFactory) {
        var transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}
