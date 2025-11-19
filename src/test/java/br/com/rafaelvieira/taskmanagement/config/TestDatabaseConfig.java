package br.com.rafaelvieira.taskmanagement.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuração de banco de dados para testes com Testcontainers O DataSource é automaticamente
 * configurado pelo @ServiceConnection do Testcontainers
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-19
 */
@Configuration
@Profile("test")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "br.com.rafaelvieira.taskmanagement.repository")
@EntityScan(basePackages = "br.com.rafaelvieira.taskmanagement.domain.model")
@EnableJpaAuditing
public class TestDatabaseConfig {
    // DataSource é automaticamente configurado pelo Testcontainers via @ServiceConnection
    // Não é necessário criar bean manualmente
}
