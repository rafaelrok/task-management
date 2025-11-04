package br.com.rafaelvieira.task_management.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuração de Testcontainers para testes de integração
 * Cria um container PostgreSQL real para os testes
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-04
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @SuppressWarnings("resource")
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16-alpine")
        )
                .withDatabaseName("test_db")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .withStartupTimeoutSeconds(60)
                .withConnectTimeoutSeconds(30);

        // Configurações de desempenho
        container.withCommand(
                "postgres",
                "-c", "fsync=off",
                "-c", "synchronous_commit=off",
                "-c", "full_page_writes=off",
                "-c", "max_connections=100"
        );

        return container;
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        // Propriedades adicionais
        registry.add("spring.test.database.replace", () -> "none");
    }
}
