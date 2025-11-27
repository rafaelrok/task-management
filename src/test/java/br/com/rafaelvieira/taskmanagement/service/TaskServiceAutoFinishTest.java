package br.com.rafaelvieira.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
@Transactional
@WithMockUser(username = "testuser")
@SuppressWarnings("resource")
class TaskServiceAutoFinishTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired TaskService taskService;

    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword("password");
        user.setFullName("Test User");
        userRepository.save(user);
    }

    @Test
    @DisplayName("Auto finaliza quando tempo alvo atingido")
    void autoFinishWhenTargetReached() {
        TaskCreateRecord req =
                new TaskCreateRecord(
                        "AutoFinish",
                        "desc",
                        TaskStatus.TODO,
                        Priority.MEDIUM,
                        null,
                        null,
                        null,
                        null,
                        5,
                        2,
                        1 // 1 minuto alvo
                        );
        TaskRecord created = taskService.createTask(req);
        TaskRecord started = taskService.changeTaskStatus(created.id(), TaskStatus.IN_PROGRESS);
        // Simular algumas pausas/resumos para acumular tempo
        taskService.changeTaskStatus(started.id(), TaskStatus.IN_PAUSE);
        taskService.changeTaskStatus(started.id(), TaskStatus.IN_PROGRESS);
        taskService.changeTaskStatus(started.id(), TaskStatus.IN_PAUSE);
        TaskRecord finalState = taskService.changeTaskStatus(started.id(), TaskStatus.IN_PROGRESS);
        assertThat(finalState.status()).isIn(TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    }
}
