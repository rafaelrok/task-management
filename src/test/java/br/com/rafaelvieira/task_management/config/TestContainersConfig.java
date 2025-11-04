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
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16-alpine")
        )
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .withStartupTimeoutSeconds(60)
                .withConnectTimeoutSeconds(30);

        // Configurações de performance
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
        // Propriedades adicionais podem ser registradas aqui
        registry.add("spring.test.database.replace", () -> "none");
    }
}

//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.utility.DockerImageName;
//
//@TestConfiguration(proxyBeanMethods = false)
//public class TestContainersConfig {
//
//    @Bean
//    @ServiceConnection
//    public PostgreSQLContainer<?> postgresContainer() {
//        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
//                .withDatabaseName("testdb")
//                .withUsername("test")
//                .withPassword("test")
//                .withReuse(true);
//    }
//}
