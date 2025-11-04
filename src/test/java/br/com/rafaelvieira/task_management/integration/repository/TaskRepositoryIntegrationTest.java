package br.com.rafaelvieira.task_management.integration.repository;

import br.com.rafaelvieira.task_management.domain.model.Category;
import br.com.rafaelvieira.task_management.domain.model.Task;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;
import br.com.rafaelvieira.task_management.domain.model.User;
import br.com.rafaelvieira.task_management.integration.BaseIntegrationTest;
import br.com.rafaelvieira.task_management.repository.CategoryRepository;
import br.com.rafaelvieira.task_management.repository.TaskRepository;
import br.com.rafaelvieira.task_management.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de Integração do Repositório usando Testcontainers
 * Demonstrando integração real com PostgreSQL
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-04
 */

@DisplayName("Task Repository Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category savedCategory;
    private User savedUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        savedCategory = categoryRepository.save(
                Category.builder()
                        .name("Work")
                        .description("Work related tasks")
                        .build()
        );

        savedUser = userRepository.save(
                User.builder()
                        .username("testuser")
                        .email("test@example.com")
                        .fullName("Test User")
                        .build()
        );
    }

    @Test
    @Order(1)
    @DisplayName("Should save and retrieve task from database")
    void shouldSaveAndRetrieveTask() {
        Task task = Task.builder()
                .title("Integration Test Task")
                .description("Testing database integration")
                .status(TaskStatus.TODO)
                .priority(Priority.HIGH)
                .category(savedCategory)
                .assignedUser(savedUser)
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();

        Task savedTask = taskRepository.save(task);
        Optional<Task> retrieved = taskRepository.findById(savedTask.getId());

        assertThat(retrieved)
                .isPresent()
                .get()
                .satisfies(t -> {
                    assertThat(t.getTitle()).isEqualTo("Integration Test Task");
                    assertThat(t.getDescription()).isEqualTo("Testing database integration");
                    assertThat(t.getStatus()).isEqualTo(TaskStatus.TODO);
                    assertThat(t.getPriority()).isEqualTo(Priority.HIGH);
                    assertThat(t.getCategory().getName()).isEqualTo("Work");
                    assertThat(t.getAssignedUser().getUsername()).isEqualTo("testuser");
                    assertThat(t.getCreatedAt()).isNotNull();
                    assertThat(t.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    @Order(2)
    @DisplayName("Should find tasks by status")
    void shouldFindTasksByStatus() {
        taskRepository.save(Task.builder()
                .title("Todo Task 1")
                .status(TaskStatus.TODO)
                .priority(Priority.LOW)
                .build());

        taskRepository.save(Task.builder()
                .title("Todo Task 2")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .build());

        taskRepository.save(Task.builder()
                .title("Done Task")
                .status(TaskStatus.DONE)
                .priority(Priority.HIGH)
                .build());

        List<Task> todoTasks = taskRepository.findByStatus(TaskStatus.TODO);
        List<Task> doneTasks = taskRepository.findByStatus(TaskStatus.DONE);

        assertThat(todoTasks).hasSize(2);
        assertThat(doneTasks).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("Should find tasks by priority")
    void shouldFindTasksByPriority() {
        taskRepository.saveAll(List.of(
                Task.builder().title("Low Priority").status(TaskStatus.TODO).priority(Priority.LOW).build(),
                Task.builder().title("High Priority 1").status(TaskStatus.TODO).priority(Priority.HIGH).build(),
                Task.builder().title("High Priority 2").status(TaskStatus.TODO).priority(Priority.HIGH).build()
        ));

        List<Task> highPriorityTasks = taskRepository.findByPriority(Priority.HIGH);
        List<Task> lowPriorityTasks = taskRepository.findByPriority(Priority.LOW);

        assertThat(highPriorityTasks).hasSize(2);
        assertThat(lowPriorityTasks).hasSize(1);
    }

    @Test
    @Order(4)
    @DisplayName("Should find tasks by category")
    void shouldFindTasksByCategory() {
        Category personalCategory = categoryRepository.save(
                Category.builder().name("Personal").build()
        );

        taskRepository.saveAll(List.of(
                Task.builder().title("Work Task 1").status(TaskStatus.TODO).priority(Priority.MEDIUM).category(savedCategory).build(),
                Task.builder().title("Work Task 2").status(TaskStatus.TODO).priority(Priority.MEDIUM).category(savedCategory).build(),
                Task.builder().title("Personal Task").status(TaskStatus.TODO).priority(Priority.MEDIUM).category(personalCategory).build()
        ));

        List<Task> workTasks = taskRepository.findByCategoryId(savedCategory.getId());
        List<Task> personalTasks = taskRepository.findByCategoryId(personalCategory.getId());

        assertThat(workTasks).hasSize(2);
        assertThat(personalTasks).hasSize(1);
    }

    @Test
    @Order(5)
    @DisplayName("Should find tasks by assigned user")
    void shouldFindTasksByAssignedUser() {
        User anotherUser = userRepository.save(
                User.builder()
                        .username("another")
                        .email("another@example.com")
                        .build()
        );

        taskRepository.saveAll(List.of(
                Task.builder().title("User 1 Task 1").status(TaskStatus.TODO).priority(Priority.MEDIUM).assignedUser(savedUser).build(),
                Task.builder().title("User 1 Task 2").status(TaskStatus.TODO).priority(Priority.MEDIUM).assignedUser(savedUser).build(),
                Task.builder().title("User 2 Task").status(TaskStatus.TODO).priority(Priority.MEDIUM).assignedUser(anotherUser).build()
        ));

        List<Task> user1Tasks = taskRepository.findByAssignedUserId(savedUser.getId());
        List<Task> user2Tasks = taskRepository.findByAssignedUserId(anotherUser.getId());

        assertThat(user1Tasks).hasSize(2);
        assertThat(user2Tasks).hasSize(1);
    }

    @Test
    @Order(6)
    @DisplayName("Should find overdue tasks")
    void shouldFindOverdueTasks() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(2);

        taskRepository.saveAll(List.of(
                Task.builder().title("Overdue Task 1").status(TaskStatus.TODO).priority(Priority.HIGH).dueDate(pastDate).build(),
                Task.builder().title("Overdue Task 2").status(TaskStatus.IN_PROGRESS).priority(Priority.URGENT).dueDate(pastDate).build(),
                Task.builder().title("Future Task").status(TaskStatus.TODO).priority(Priority.MEDIUM).dueDate(futureDate).build(),
                Task.builder().title("Completed Task").status(TaskStatus.DONE).priority(Priority.LOW).dueDate(pastDate).build()
        ));

        List<Task> overdueTasks = taskRepository.findOverdueTasks(LocalDateTime.now());

        assertThat(overdueTasks)
                .hasSize(2)
                .allSatisfy(task -> {
                    assertThat(task.getDueDate()).isBefore(LocalDateTime.now());
                    assertThat(task.getStatus()).isNotEqualTo(TaskStatus.DONE);
                });
    }

    @Test
    @Order(7)
    @DisplayName("Should count tasks by status")
    void shouldCountTasksByStatus() {
        taskRepository.saveAll(List.of(
                Task.builder().title("Todo 1").status(TaskStatus.TODO).priority(Priority.LOW).build(),
                Task.builder().title("Todo 2").status(TaskStatus.TODO).priority(Priority.LOW).build(),
                Task.builder().title("Todo 3").status(TaskStatus.TODO).priority(Priority.LOW).build(),
                Task.builder().title("Done 1").status(TaskStatus.DONE).priority(Priority.LOW).build()
        ));

        long todoCount = taskRepository.countByStatus(TaskStatus.TODO);
        long doneCount = taskRepository.countByStatus(TaskStatus.DONE);

        assertThat(todoCount).isEqualTo(3);
        assertThat(doneCount).isEqualTo(1);
    }

    @Test
    @Order(8)
    @DisplayName("Should find tasks by user and status")
    void shouldFindTasksByUserAndStatus() {
        taskRepository.saveAll(List.of(
                Task.builder().title("User Todo 1").status(TaskStatus.TODO).priority(Priority.LOW).assignedUser(savedUser).build(),
                Task.builder().title("User Todo 2").status(TaskStatus.TODO).priority(Priority.LOW).assignedUser(savedUser).build(),
                Task.builder().title("User Done").status(TaskStatus.DONE).priority(Priority.LOW).assignedUser(savedUser).build()
        ));

        List<Task> userTodoTasks = taskRepository.findByUserIdAndStatus(savedUser.getId(), TaskStatus.TODO);
        List<Task> userDoneTasks = taskRepository.findByUserIdAndStatus(savedUser.getId(), TaskStatus.DONE);

        assertThat(userTodoTasks).hasSize(2);
        assertThat(userDoneTasks).hasSize(1);
    }

    @Test
    @Order(9)
    @DisplayName("Should find tasks by title containing")
    void shouldFindTasksByTitleContaining() {
        taskRepository.saveAll(List.of(
                Task.builder().title("Review code").status(TaskStatus.TODO).priority(Priority.MEDIUM).build(),
                Task.builder().title("Code refactoring").status(TaskStatus.TODO).priority(Priority.HIGH).build(),
                Task.builder().title("Write documentation").status(TaskStatus.TODO).priority(Priority.LOW).build()
        ));

        List<Task> tasksWithCode = taskRepository.findByTitleContainingIgnoreCase("code");

        assertThat(tasksWithCode)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Review code", "Code refactoring");
    }

    @Test
    @Order(10)
    @DisplayName("Should update task and verify updated timestamp")
    void shouldUpdateTaskAndVerifyTimestamp() throws InterruptedException {
        Task task = taskRepository.save(
                Task.builder()
                        .title("Original Title")
                        .status(TaskStatus.TODO)
                        .priority(Priority.LOW)
                        .build()
        );

        LocalDateTime originalUpdatedAt = task.getUpdatedAt();
        Thread.sleep(1000); // Delay to ensure timestamp difference

        task.setTitle("Updated Title");
        Task updatedTask = taskRepository.save(task);

        assertThat(updatedTask.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedTask.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(updatedTask.getCreatedAt()).isEqualTo(task.getCreatedAt());
    }
}