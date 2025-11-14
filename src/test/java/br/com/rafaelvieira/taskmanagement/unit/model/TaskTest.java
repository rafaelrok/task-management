package br.com.rafaelvieira.taskmanagement.unit.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes unitários do modelo Task Testando lógica de negócio e comportamentos
 *
 * @author Rafael Vieira
 * @since 02/11/2025
 */
@DisplayName("Task Model Tests")
class TaskTest {

    @Test
    @DisplayName("Should create task with builder")
    void testShouldCreateTaskWithBuilder() {
        Task task =
                Task.builder()
                        .id(1L)
                        .title("Test Task")
                        .description("Description")
                        .status(TaskStatus.TODO)
                        .priority(Priority.HIGH)
                        .build();

        assertAll(
                "Task Properties",
                () -> assertEquals(1L, task.getId(), "id deve ser 1"),
                () -> assertEquals("Test Task", task.getTitle(), "título deve ser 'Test Task'"),
                () ->
                        assertEquals(
                                "Description",
                                task.getDescription(),
                                "descrição deve ser 'Description'"),
                () -> assertEquals(TaskStatus.TODO, task.getStatus(), "status deve ser TODO"),
                () -> assertEquals(Priority.HIGH, task.getPriority(), "prioridade deve ser HIGH"));
    }

    @Test
    @DisplayName("Should detect overdue task")
    void testShouldDetectOverdueTask() {
        Task task =
                Task.builder()
                        .title("Overdue Task")
                        .status(TaskStatus.TODO)
                        .priority(Priority.HIGH)
                        .dueDate(LocalDateTime.now().minusDays(1))
                        .build();

        assertThat(task.isOverdue()).as("Check if task is overdue").isTrue();
    }

    @Test
    @DisplayName("Should not detect overdue for future tasks")
    void testShouldNotDetectOverdueForFutureTasks() {
        Task task =
                Task.builder()
                        .title("Future Task")
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .dueDate(LocalDateTime.now().plusDays(7))
                        .build();

        assertThat(task.isOverdue()).as("Check if future task is not overdue").isFalse();
    }

    @Test
    @DisplayName("Should not detect overdue for completed tasks")
    void testShouldNotDetectOverdueForCompletedTasks() {
        Task task =
                Task.builder()
                        .title("Completed Task")
                        .status(TaskStatus.DONE)
                        .priority(Priority.LOW)
                        .dueDate(LocalDateTime.now().minusDays(1))
                        .build();

        assertThat(task.isOverdue()).as("Check if completed task is not overdue").isFalse();
    }

    @Test
    @DisplayName("Should detect completed task")
    void testShouldDetectCompletedTask() {
        Task task =
                Task.builder()
                        .title("Done Task")
                        .status(TaskStatus.DONE)
                        .priority(Priority.MEDIUM)
                        .build();

        assertThat(task.isCompleted()).as("Check if task is completed").isTrue();
    }

    @ParameterizedTest
    @CsvSource({"TODO, false", "IN_PROGRESS, false", "DONE, true", "CANCELLED, false"})
    @DisplayName("Should correctly identify completed status")
    void testShouldCorrectlyIdentifyCompletedStatus(TaskStatus status, boolean expectedCompleted) {
        Task task = Task.builder().title("Task").status(status).priority(Priority.MEDIUM).build();

        assertThat(task.isCompleted())
                .as("Check if task with status %s is completed", status)
                .isEqualTo(expectedCompleted);
    }

    @Test
    @DisplayName("Should handle null due date")
    void testShouldHandleNullDueDate() {
        Task task =
                Task.builder()
                        .title("No Due Date Task")
                        .status(TaskStatus.TODO)
                        .priority(Priority.LOW)
                        .dueDate(null)
                        .build();

        assertThat(task.isOverdue())
                .as("Check if task with null due date is not overdue")
                .isFalse();
    }
}
