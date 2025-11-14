package br.com.rafaelvieira.taskmanagement.config;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * Configuração do Flyway para migrações de banco de dados
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-05
 */
@Configuration
@ConditionalOnProperty(
        prefix = "spring.flyway",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class FlywayConfig {

    @Bean(initMethod = "migrate")

    @Profile("!test")
    @Order(HIGHEST_PRECEDENCE)
    public Flyway flyway(DataSource dataSource) {
        log.info("========================================");
        log.info("Configurando e executando Flyway...");
        log.info("========================================");

        var flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .baselineVersion("0")
                        .validateOnMigrate(true)
                        .cleanDisabled(false)
                        .outOfOrder(false)
                        .load();

        flyway.repair();

        log.info("Flyway configurado!");
        log.info("Localização: classpath:db/migration");
        log.info("Executando migrações...");

        return flyway;
    }
}
