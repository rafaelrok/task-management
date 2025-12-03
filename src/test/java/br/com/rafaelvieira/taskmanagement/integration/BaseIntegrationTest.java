package br.com.rafaelvieira.taskmanagement.integration;

import br.com.rafaelvieira.taskmanagement.config.TestAsyncConfig;
import br.com.rafaelvieira.taskmanagement.domain.enums.Role;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Transactional
@Import(TestAsyncConfig.class)
public abstract class BaseIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired protected TaskRepository taskRepository;

    @Autowired protected CategoryRepository categoryRepository;

    @Autowired protected UserRepository userRepository;

    @Autowired protected ObjectMapper objectMapper;
    protected MockMvc mockMvc;
    protected User defaultAdminUser;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired private WebApplicationContext context;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(SecurityMockMvcConfigurers.springSecurity())
                        .build();
        defaultAdminUser =
                userRepository.save(
                        User.builder()
                                .username("integration-admin")
                                .fullName("Integration Admin")
                                .email("integration-admin@example.com")
                                .password(passwordEncoder.encode("password"))
                                .role(Role.ADMIN)
                                .build());
    }

    protected RequestPostProcessor authenticateAdmin() {
        return SecurityMockMvcRequestPostProcessors.user("integration-admin")
                .password("password")
                .roles("ADMIN");
    }

    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
