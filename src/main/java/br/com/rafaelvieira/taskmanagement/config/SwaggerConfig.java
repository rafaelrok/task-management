package br.com.rafaelvieira.taskmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
public class SwaggerConfig {

    private static final String API_TITLE = "Task Management API";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION =
            """
            API completa para gerenciamento de tarefas

            Desenvolvida com:
            - Java 25
            - Spring Boot 3.5.7
            - JUnit 6.0.0
            - PostgresSQL

            Funcionalidades:
            - CRUD completo de tarefas
            - Sistema de categorias
            - Atribuição de usuários
            - Prioridades (LOW, MEDIUM, HIGH, URGENT)
            - Status (TODO, IN_PROGRESS, DONE, CANCELLED)
            - Detector de tarefas atrasadas
            """;

    private static final String CONTACT_NAME = "Rafael Vieira";
    private static final String CONTACT_EMAIL = "rafaelvieiradev@gmail.com";
    private static final String CONTACT_URL = "https://rafaelvieiradev.com.br";
    private static final String GITHUB_URL = "https://github.com/rafaelrok";

    private static final String LICENSE_NAME = "MIT License";
    private static final String LICENSE_URL =
            "https://github.com/rafaelrok/task-management/blob/main/LICENSE";

    private static final String SERVER_DESCRIPTION = "Servidor de Desenvolvimento";
    private static final String SERVER_URL_TEMPLATE = "http://localhost:%s";

    @Value("${server.port:8080}")

    private String serverPort;

    private static Info buildApiInfo() {
        return new Info()
                .title(API_TITLE)
                .version(API_VERSION)
                .description(API_DESCRIPTION)
                .contact(buildContact())
                .license(buildLicense());
    }

    private static Contact buildContact() {
        return new Contact()
                .name(CONTACT_NAME)
                .email(CONTACT_EMAIL)
                .url(GITHUB_URL)
                .url(CONTACT_URL);
    }

    private static License buildLicense() {
        return new License().name(LICENSE_NAME).url(LICENSE_URL);
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI().info(buildApiInfo()).servers(buildServers());
    }

    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url(String.format(SERVER_URL_TEMPLATE, serverPort))
                        .description(SERVER_DESCRIPTION));
    }
}
