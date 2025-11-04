package br.com.rafaelvieira.task_management.integration.controller;


import br.com.rafaelvieira.task_management.domain.dto.TaskCreateRecord;
import br.com.rafaelvieira.task_management.domain.model.Category;
import br.com.rafaelvieira.task_management.domain.model.Task;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;
import br.com.rafaelvieira.task_management.domain.model.User;
import br.com.rafaelvieira.task_management.integration.BaseIntegrationTest;
import br.com.rafaelvieira.task_management.repository.CategoryRepository;
import br.com.rafaelvieira.task_management.repository.TaskRepository;
import br.com.rafaelvieira.task_management.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Integração do Repositório
 * Estende BaseIntegrationTest para reutilizar configurações
 *
 * @author Rafael Vieira (rafaelrok)
 * @since 2025-11-04
 */

@DisplayName("Task Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Category testCategory;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testCategory = categoryRepository.save(
                Category.builder()
                        .name("Development")
                        .description("Development tasks")
                        .build()
        );

        testUser = userRepository.save(
                User.builder()
                        .username("developer")
                        .email("dev@example.com")
                        .fullName("Developer User")
                        .build()
        );
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/tasks - Should create task successfully")
    void testShouldCreateTaskSuccessfully() throws Exception {
        TaskCreateRecord create = new TaskCreateRecord(
                "Implement new feature",
                "Implement user authentication",
                TaskStatus.TODO,
                Priority.HIGH,
                testCategory.getId(),
                testUser.getId(),
                LocalDateTime.now().plusDays(7)
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Implement new feature"))
                .andExpect(jsonPath("$.description").value("Implement user authentication"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.categoryName").value("Development"))
                .andExpect(jsonPath("$.assignedUserName").value("developer"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/tasks - Should return 400 when title is blank")
    void testShouldReturn400WhenTitleIsBlank() throws Exception {
        TaskCreateRecord create = new TaskCreateRecord(
                "",
                "Description",
                TaskStatus.TODO,
                Priority.MEDIUM,
                null,
                null,
                null    
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/tasks/{id} - Should get task by ID")
    void testShouldGetTaskById() throws Exception {
        Task task = taskRepository.save(
                Task.builder()
                        .title("Test Task")
                        .description("Test Description")
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .build()
        );

        mockMvc.perform(get("/api/tasks/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/tasks/{id} - Should return 404 when task not found")
    void testShouldReturn404WhenTaskNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Task not found")));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/tasks - Should get all tasks")
    void testShouldGetAllTasks() throws Exception {
        taskRepository.save(Task.builder().title("Task 1").status(TaskStatus.TODO).priority(Priority.LOW).build());
        taskRepository.save(Task.builder().title("Task 2").status(TaskStatus.IN_PROGRESS).priority(Priority.MEDIUM).build());
        taskRepository.save(Task.builder().title("Task 3").status(TaskStatus.DONE).priority(Priority.HIGH).build());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Task 1", "Task 2", "Task 3")));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/tasks/status/{status} - Should get tasks by status")
    void testShouldGetTasksByStatus() throws Exception {
        taskRepository.save(Task.builder().title("Todo 1").status(TaskStatus.TODO).priority(Priority.LOW).build());
        taskRepository.save(Task.builder().title("Todo 2").status(TaskStatus.TODO).priority(Priority.LOW).build());
        taskRepository.save(Task.builder().title("Done 1").status(TaskStatus.DONE).priority(Priority.LOW).build());

        mockMvc.perform(get("/api/tasks/status/TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].status", everyItem(is("TODO"))));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/tasks/priority/{priority} - Should get tasks by priority")
    void testShouldGetTasksByPriority() throws Exception {
        taskRepository.save(Task.builder().title("High 1").status(TaskStatus.TODO).priority(Priority.HIGH).build());
        taskRepository.save(Task.builder().title("High 2").status(TaskStatus.TODO).priority(Priority.HIGH).build());
        taskRepository.save(Task.builder().title("Low 1").status(TaskStatus.TODO).priority(Priority.LOW).build());

        mockMvc.perform(get("/api/tasks/priority/HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].priority", everyItem(is("HIGH"))));
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/tasks/category/{categoryId} - Should get tasks by category")
    void testShouldGetTasksByCategory() throws Exception {
        taskRepository.save(Task.builder().title("Dev Task 1").status(TaskStatus.TODO).priority(Priority.MEDIUM).category(testCategory).build());
        taskRepository.save(Task.builder().title("Dev Task 2").status(TaskStatus.TODO).priority(Priority.MEDIUM).category(testCategory).build());

        mockMvc.perform(get("/api/tasks/category/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].categoryName", everyItem(is("Development"))));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/tasks/user/{userId} - Should get tasks by user")
    void testShouldGetTasksByUser() throws Exception {
        taskRepository.save(Task.builder().title("User Task 1").status(TaskStatus.TODO).priority(Priority.MEDIUM).assignedUser(testUser).build());
        taskRepository.save(Task.builder().title("User Task 2").status(TaskStatus.TODO).priority(Priority.MEDIUM).assignedUser(testUser).build());

        mockMvc.perform(get("/api/tasks/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].assignedUserName", everyItem(is("developer"))));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/tasks/overdue - Should get overdue tasks")
    void testShouldGetOverdueTasks() throws Exception {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(2);

        taskRepository.save(Task.builder().title("Overdue 1").status(TaskStatus.TODO).priority(Priority.HIGH).dueDate(pastDate).build());
        taskRepository.save(Task.builder().title("Overdue 2").status(TaskStatus.IN_PROGRESS).priority(Priority.URGENT).dueDate(pastDate).build());
        taskRepository.save(Task.builder().title("Future").status(TaskStatus.TODO).priority(Priority.LOW).dueDate(LocalDateTime.now().plusDays(2)).build());

        mockMvc.perform(get("/api/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].overdue", everyItem(is(true))));
    }

    @Test
    @Order(11)
    @DisplayName("PUT /api/tasks/{id} - Should update task")
    void testShouldUpdateTask() throws Exception {
        Task task = taskRepository.save(
                Task.builder()
                        .title("Original Title")
                        .description("Original Description")
                        .status(TaskStatus.TODO)
                        .priority(Priority.LOW)
                        .build()
        );

        TaskCreateRecord update = new TaskCreateRecord(
                "Updated Title",
                "Updated Description",
                TaskStatus.IN_PROGRESS,
                Priority.HIGH,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @Order(12)
    @DisplayName("PATCH /api/tasks/{id}/status - Should change task status")
    void testShouldChangeTaskStatus() throws Exception {
        Task task = taskRepository.save(
                Task.builder()
                        .title("Task")
                        .status(TaskStatus.TODO)
                        .priority(Priority.MEDIUM)
                        .build()
        );

        mockMvc.perform(patch("/api/tasks/" + task.getId() + "/status")
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /api/tasks/{id} - Should delete task")
    void testShouldDeleteTask() throws Exception {
        Task task = taskRepository.save(
                Task.builder()
                        .title("Task to delete")
                        .status(TaskStatus.TODO)
                        .priority(Priority.LOW)
                        .build()
        );

        mockMvc.perform(delete("/api/tasks/" + task.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + task.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/tasks/count/{status} - Should count tasks by status")
    void testShouldCountTasksByStatus() throws Exception {
        taskRepository.save(Task.builder().title("Todo 1").status(TaskStatus.TODO).priority(Priority.LOW).build());
        taskRepository.save(Task.builder().title("Todo 2").status(TaskStatus.TODO).priority(Priority.LOW).build());
        taskRepository.save(Task.builder().title("Todo 3").status(TaskStatus.TODO).priority(Priority.LOW).build());

        mockMvc.perform(get("/api/tasks/count/TODO"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
