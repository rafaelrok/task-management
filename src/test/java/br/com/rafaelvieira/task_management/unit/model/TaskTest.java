package br.com.rafaelvieira.task_management.unit.model;

import br.com.rafaelvieira.task_management.domain.model.Task;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do modelo Task
 * Testando lógica de negócio e comportamentos
 */
@DisplayName("Task Model Tests")
class TaskTest {

    @Test
    @DisplayName("Should create task with builder")
    void shouldCreateTaskWithBuilder() {
        Task task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Description")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .build();

        assertAll("Task Properties",
                () -> assertEquals(1L, task.getId()),
                () -> assertEquals("Test Task", task.getTitle()),
                () -> assertEquals("Description", task.getDescription()),
                () -> assertEquals(TaskStatus.TODO, task.getStatus()),
                () -> assertEquals(Priority.HIGH, task.getPriority())
        );
    }

    @Test
    @DisplayName("Should detect overdue task")
    void shouldDetectOverdueTask() {
        Task task = Task.builder()
                .title("Overdue Task")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .dueDate(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(task.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("Should not detect overdue for future tasks")
    void shouldNotDetectOverdueForFutureTasks() {
        Task task = Task.builder()
                .title("Future Task")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();

        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Should not detect overdue for completed tasks")
    void shouldNotDetectOverdueForCompletedTasks() {
        Task task = Task.builder()
                .title("Completed Task")
                .status(TaskStatus.DONE)
                .priority(Priority.LOW)
                .dueDate(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(task.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Should detect completed task")
    void shouldDetectCompletedTask() {
        Task task = Task.builder()
                .title("Done Task")
                .status(TaskStatus.DONE)
                .priority(Priority.MEDIUM)
                .build();

        assertThat(task.isCompleted()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "TODO, false",
            "IN_PROGRESS, false",
            "DONE, true",
            "CANCELLED, false"
    })
    @DisplayName("Should correctly identify completed status")
    void shouldCorrectlyIdentifyCompletedStatus(TaskStatus status, boolean expectedCompleted) {
        Task task = Task.builder()
                .title("Task")
                .status(status)
                .priority(Priority.MEDIUM)
                .build();

        assertThat(task.isCompleted()).isEqualTo(expectedCompleted);
    }

    @Test
    @DisplayName("Should handle null due date")
    void shouldHandleNullDueDate() {
        Task task = Task.builder()
                .title("No Due Date Task")
                .status(TaskStatus.TODO)
                .priority(Priority.LOW)
                .dueDate(null)
                .build();

        assertThat(task.isOverdue()).isFalse();
    }
}