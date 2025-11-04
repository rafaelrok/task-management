package br.com.rafaelvieira.task_management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Management API")
                        .version("1.0.0")
                        .description("API completa para gerenciamento de tarefas\n\n" +
                                "Desenvolvida com:\n" +
                                "- Java 25\n" +
                                "- Spring Boot 4.0.0-SNAPSHOT\n" +
                                "- JUnit 6.0.0\n" +
                                "- PostgreSQL\n\n" +
                                "Funcionalidades:\n" +
                                "- CRUD completo de tarefas\n" +
                                "- Sistema de categorias\n" +
                                "- Atribuição de usuários\n" +
                                "- Prioridades (LOW, MEDIUM, HIGH, URGENT)\n" +
                                "- Status (TODO, IN_PROGRESS, DONE, CANCELLED)\n" +
                                "- Detecção de tarefas atrasadas")
                        .contact(new Contact()
                                .name("Rafael Vieira")
                                .url("https://rafaelvieiradev.com.br")
                                .email("rafaelvieiradev@gmail.com")
                                .url("https://github.com/rafaelrok"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://github.com/rafaelrok/task-management/blob/main/LICENSE")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de Desenvolvimento")
                ));
    }
}
