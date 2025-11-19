package br.com.rafaelvieira.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
@SuppressWarnings("resource")
class TaskServiceStatusChangeTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired TaskService taskService;

    @Test
    @DisplayName("Nao inicia sem executionTimeMinutes e pomodoroMinutes")
    void shouldStayTodoIfMissingExecutionOrPomodoro() {
        TaskCreateRecord req =
                new TaskCreateRecord(
                        "Teste",
                        "desc",
                        TaskStatus.TODO,
                        Priority.MEDIUM,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        TaskRecord created = taskService.createTask(req);
        TaskRecord changed = taskService.changeTaskStatus(created.id(), TaskStatus.IN_PROGRESS);
        assertThat(changed.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @DisplayName("Inicia quando campos presentes")
    void shouldStartWhenFieldsPresent() {
        TaskCreateRecord req2 =
                new TaskCreateRecord(
                        "Teste2",
                        "desc",
                        TaskStatus.TODO,
                        Priority.MEDIUM,
                        null,
                        null,
                        null,
                        null,
                        25,
                        5,
                        120);
        TaskRecord created = taskService.createTask(req2);
        TaskRecord changed = taskService.changeTaskStatus(created.id(), TaskStatus.IN_PROGRESS);
        assertThat(changed.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
