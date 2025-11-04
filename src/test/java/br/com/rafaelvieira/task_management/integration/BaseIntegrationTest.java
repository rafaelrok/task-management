package br.com.rafaelvieira.task_management.integration;

import br.com.rafaelvieira.task_management.config.TestContainersConfig;
import br.com.rafaelvieira.task_management.repository.CategoryRepository;
import br.com.rafaelvieira.task_management.repository.TaskRepository;
import br.com.rafaelvieira.task_management.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classe base para testes de integração
 * Fornece configuração comum e utilitários para todos os testes de integração
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-04
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected void logDebug(String message) {
        System.out.println("[TEST] " + message);
    }

    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
