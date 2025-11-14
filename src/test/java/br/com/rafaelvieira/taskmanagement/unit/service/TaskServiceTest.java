package br.com.rafaelvieira.taskmanagement.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Category;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import br.com.rafaelvieira.taskmanagement.service.impl.TaskServiceImpl;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testes unitários do serviço Task Service Testando lógica de negócio e interações com o
 * repositório
 *
 * @author Rafael Vieira
 * @since 02/11/2025
 */
@DisplayName("Task Service Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    @Order(1)
    @DisplayName("Should create task successfully")
    void testShouldCreateTaskSuccessfully() {
        TaskCreateRecord create =
                new TaskCreateRecord(
                        "Test Task", "Test Description", null, Priority.HIGH, null, null, null);

        Task task =
                Task.builder()
                        .id(1L)
                        .title("Test Task")
                        .description("Test Description")
                        .status(TaskStatus.TODO)
                        .priority(Priority.HIGH)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskRecord result = taskService.createTask(create);

        assertAll(
                "Task Creation Assertions",
                () -> assertNotNull(result, "Created Task should not be null"),
                () -> assertEquals("Test Task", result.title(), "Title should match"),
                () ->
                        assertEquals(
                                "Test Description",
                                result.description(),
                                "Description should match"),
                () -> assertEquals(Priority.HIGH, result.priority(), "Priority should match"),
                () -> assertEquals(TaskStatus.TODO, result.status(), "Status should be TODO"));

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @Order(2)
    @DisplayName("Should create task with category")
    void testShouldCreateTaskWithCategory() {
        Category category = Category.builder().id(1L).name("Work").build();

        TaskCreateRecord create =
                new TaskCreateRecord("Work Task", "Work Description", null, null, 1L, null, null);

        Task task =
                Task.builder()
                        .id(1L)
                        .title("Work Task")
                        .description("Work Description")
                        .category(category)
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskRecord result = taskService.createTask(create);

        assertThat(result)
                .as("Created Task")
                .isNotNull()
                .satisfies(
                        t -> {
                            assertThat(t.title()).as("Title").isEqualTo("Work Task");
                            assertThat(t.categoryName()).as("Category Name").isEqualTo("Work");
                        });

        verify(categoryRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @Order(3)
    @DisplayName("Should throw exception when category not found")
    void testShouldThrowExceptionWhenCategoryNotFound() {
        TaskCreateRecord create =
                new TaskCreateRecord("Task", "Description", null, null, 999L, null, null);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(create))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found with id: 999");

        verify(taskRepository, never()).save(any(Task.class));
    }

    // ========== TESTES PARAMETRIZADOS (JUnit 6) ==========

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    @DisplayName("Should find tasks by all possible statuses")
    void testShouldFindTasksByAllStatuses(TaskStatus status) {
        Task task =
                Task.builder()
                        .id(1L)
                        .title("Task")
                        .status(status)
                        .priority(Priority.MEDIUM)
                        .build();

        when(taskRepository.findByStatus(status)).thenReturn(List.of(task));

        List<TaskRecord> result = taskService.getTasksByStatus(status);

        assertThat(result)
                .as("Tasks with status " + status)
                .isNotEmpty()
                .hasSize(1)
                .allSatisfy(t -> assertThat(t.status()).as("Status").isEqualTo(status));

        verify(taskRepository, times(1)).findByStatus(status);
    }

    @ParameterizedTest
    @EnumSource(Priority.class)
    @DisplayName("Should find tasks by all priorities")
    void testShouldFindTasksByAllPriorities(Priority priority) {
        Task task =
                Task.builder()
                        .id(1L)
                        .title("Task")
                        .status(TaskStatus.TODO)
                        .priority(priority)
                        .build();

        when(taskRepository.findByPriority(priority)).thenReturn(List.of(task));

        List<TaskRecord> result = taskService.getTasksByPriority(priority);

        assertThat(result)
                .as("Tasks with priority " + priority)
                .isNotEmpty()
                .extracting(TaskRecord::priority)
                .containsOnly(priority);

        verify(taskRepository, times(1)).findByPriority(priority);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L, 100L})
    @DisplayName("Should get task by different IDs")
    void testShouldGetTaskByDifferentIds(Long id) {
        Task task =
                Task.builder()
                        .id(id)
                        .title("Task " + id)
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .build();

        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        TaskRecord result = taskService.getTaskById(id);

        assertThat(result.id()).as("Task ID").isEqualTo(id);
        verify(taskRepository, times(1)).findById(id);
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @Order(4)
    @DisplayName("Should update task successfully")
    void testShouldUpdateTaskSuccessfully() {
        Long taskId = 1L;
        Task existingTask =
                Task.builder()
                        .id(taskId)
                        .title("Old Title")
                        .description("Old Description")
                        .status(TaskStatus.TODO)
                        .priority(Priority.LOW)
                        .build();

        TaskCreateRecord update =
                new TaskCreateRecord(
                        "New Title",
                        "New Description",
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        null,
                        null,
                        null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskRecord result = taskService.updateTask(taskId, update);

        assertAll(
                "Update Assertions",
                () -> assertEquals("New Title", result.title(), "Title should be updated"),
                () ->
                        assertEquals(
                                "New Description",
                                result.description(),
                                "Description should be updated"),
                () ->
                        assertEquals(
                                TaskStatus.IN_PROGRESS,
                                result.status(),
                                "Status should be updated"),
                () -> assertEquals(Priority.HIGH, result.priority(), "Priority should be updated"));

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @Order(5)
    @DisplayName("Should change task status")
    void testShouldChangeTaskStatus() {
        Long taskId = 1L;
        Task task =
                Task.builder()
                        .id(taskId)
                        .title("Task")
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskRecord result = taskService.changeTaskStatus(taskId, TaskStatus.DONE);

        assertThat(result.status()).as("Updated Status").isEqualTo(TaskStatus.DONE);
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    // ========== TESTES DE DELEÇÃO ==========

    @Test
    @Order(6)
    @DisplayName("Should delete task successfully")
    void testShouldDeleteTaskSuccessfully() {
        Long taskId = 1L;
        when(taskRepository.existsById(taskId)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(taskId);

        assertDoesNotThrow(
                () -> taskService.deleteTask(taskId), "Deleting existing task should not throw");

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    @Order(7)
    @DisplayName("Should throw exception when deleting non-existent task")
    void testShouldThrowExceptionWhenDeletingNonExistentTask() {
        Long taskId = 999L;
        when(taskRepository.existsById(taskId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with id: 999");

        verify(taskRepository, never()).deleteById(anyLong());
    }

    // ========== TESTES DE LISTAGEM ==========

    @Test
    @Order(8)
    @DisplayName("Should get all tasks")
    void testShouldGetAllTasks() {
        List<Task> tasks =
                Arrays.asList(
                        Task.builder()
                                .id(1L)
                                .title("Task 1")
                                .status(TaskStatus.TODO)
                                .priority(Priority.LOW)
                                .build(),
                        Task.builder()
                                .id(2L)
                                .title("Task 2")
                                .status(TaskStatus.IN_PROGRESS)
                                .priority(Priority.MEDIUM)
                                .build(),
                        Task.builder()
                                .id(3L)
                                .title("Task 3")
                                .status(TaskStatus.DONE)
                                .priority(Priority.HIGH)
                                .build());

        when(taskRepository.findAll()).thenReturn(tasks);

        List<TaskRecord> result = taskService.getAllTasks();

        assertThat(result)
                .as("All Tasks")
                .hasSize(3)
                .extracting(TaskRecord::title)
                .containsExactly("Task 1", "Task 2", "Task 3");

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    @Order(9)
    @DisplayName("Should get tasks by user ID")
    void testShouldGetTasksByUserId() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("testuser").build();

        List<Task> tasks =
                Arrays.asList(
                        Task.builder()
                                .id(1L)
                                .title("Task 1")
                                .assignedUser(user)
                                .status(TaskStatus.TODO)
                                .priority(Priority.MEDIUM)
                                .build(),
                        Task.builder()
                                .id(2L)
                                .title("Task 2")
                                .assignedUser(user)
                                .status(TaskStatus.TODO)
                                .priority(Priority.MEDIUM)
                                .build());

        when(taskRepository.findByAssignedUserId(userId)).thenReturn(tasks);

        List<TaskRecord> result = taskService.getTasksByUserId(userId);

        assertThat(result)
                .as("Tasks assigned to user ID " + userId)
                .hasSize(2)
                .allSatisfy(
                        t ->
                                assertThat(t.assignedUserName())
                                        .as("Assigned User Name")
                                        .isEqualTo("testuser"));

        verify(taskRepository, times(1)).findByAssignedUserId(userId);
    }

    // ========== TESTES DE TAREFAS ATRASADAS ==========

    @Test
    @Order(10)
    @DisplayName("Should find overdue tasks")
    void testShouldFindOverdueTasks() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        List<Task> overdueTasks =
                Arrays.asList(
                        Task.builder()
                                .id(1L)
                                .title("Overdue Task 1")
                                .status(TaskStatus.TODO)
                                .priority(Priority.HIGH)
                                .dueDate(pastDate)
                                .build(),
                        Task.builder()
                                .id(2L)
                                .title("Overdue Task 2")
                                .status(TaskStatus.IN_PROGRESS)
                                .priority(Priority.URGENT)
                                .dueDate(pastDate)
                                .build());

        when(taskRepository.findOverdueTasks(any(LocalDateTime.class))).thenReturn(overdueTasks);

        List<TaskRecord> result = taskService.getOverdueTasks();

        assertThat(result)
                .as("Overdue Tasks")
                .hasSize(2)
                .allSatisfy(t -> assertThat(t.overdue()).as("Is Overdue").isTrue());

        verify(taskRepository, times(1)).findOverdueTasks(any(LocalDateTime.class));
    }

    // ========== TESTES DE CONTAGEM ==========

    @Test
    @Order(11)
    @DisplayName("Should count tasks by status")
    void testShouldCountTasksByStatus() {
        TaskStatus status = TaskStatus.TODO;
        when(taskRepository.countByStatus(status)).thenReturn(5L);

        long count = taskService.countTasksByStatus(status);

        assertThat(count).as("Count of tasks with status " + status).isEqualTo(5L);
        verify(taskRepository, times(1)).countByStatus(status);
    }

    // ========== TESTE NESTED (JUnit 6) ==========

    @Nested
    @DisplayName("Task with User Assignment Tests")
    class TaskWithUserTests {

        @Test
        @DisplayName("Should create task with assigned user")
        void testShouldCreateTaskWithAssignedUser() {
            User user =
                    User.builder().id(1L).username("developer").email("dev@example.com").build();

            TaskCreateRecord create =
                    new TaskCreateRecord(
                            "Development Task", "Code review", null, null, null, 1L, null);

            Task task =
                    Task.builder()
                            .id(1L)
                            .title("Development Task")
                            .description("Code review")
                            .assignedUser(user)
                            .status(TaskStatus.TODO)
                            .priority(Priority.MEDIUM)
                            .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            TaskRecord result = taskService.createTask(create);

            assertThat(result)
                    .as("Created Task with Assigned User")
                    .isNotNull()
                    .satisfies(
                            t -> {
                                assertThat(t.assignedUserName())
                                        .as("Assigned User Name")
                                        .isEqualTo("developer");
                                assertThat(t.title()).as("Title").isEqualTo("Development Task");
                            });

            verify(userRepository, times(1)).findById(1L);
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void testShouldThrowExceptionWhenUserNotFound() {
            TaskCreateRecord create =
                    new TaskCreateRecord("Task", "Description", null, null, null, 999L, null);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(create))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }
    }
}
