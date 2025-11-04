package br.com.rafaelvieira.task_management.service.impl;

import br.com.rafaelvieira.task_management.domain.dto.TaskCreateRecord;
import br.com.rafaelvieira.task_management.domain.dto.TaskRecord;
import br.com.rafaelvieira.task_management.domain.enums.Priority;
import br.com.rafaelvieira.task_management.domain.enums.TaskStatus;
import br.com.rafaelvieira.task_management.domain.model.Category;
import br.com.rafaelvieira.task_management.domain.model.Task;
import br.com.rafaelvieira.task_management.domain.model.User;
import br.com.rafaelvieira.task_management.exception.ResourceNotFoundException;
import br.com.rafaelvieira.task_management.repository.CategoryRepository;
import br.com.rafaelvieira.task_management.repository.TaskRepository;
import br.com.rafaelvieira.task_management.repository.UserRepository;
import br.com.rafaelvieira.task_management.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public TaskRecord createTask(TaskCreateRecord taskCreate) {
        Task task = Task.builder()
                .title(taskCreate.title())
                .description(taskCreate.description())
                .status(taskCreate.status() != null ? taskCreate.status() : TaskStatus.TODO)
                .priority(taskCreate.priority() != null ? taskCreate.priority() : Priority.MEDIUM)
                .dueDate(taskCreate.dueDate())
                .build();

        if (taskCreate.categoryId() != null) {
            Category category = categoryRepository.findById(taskCreate.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + taskCreate.categoryId()));
            task.setCategory(category);
        }

        if (taskCreate.assignedUserId() != null) {
            User user = userRepository.findById(taskCreate.assignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + taskCreate.assignedUserId()));
            task.setAssignedUser(user);
        }

        Task savedTask = taskRepository.save(task);
        return convertToDTO(savedTask);
    }

    @Override
    public TaskRecord updateTask(Long id, TaskCreateRecord taskCreate) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status() != null ? taskCreate.status() : task.getStatus());
        task.setPriority(taskCreate.priority() != null ? taskCreate.priority() : task.getPriority());
        task.setDueDate(taskCreate.dueDate());

        if (taskCreate.categoryId() != null) {
            Category category = categoryRepository.findById(taskCreate.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + taskCreate.categoryId()));
            task.setCategory(category);
        }

        if (taskCreate.assignedUserId() != null) {
            User user = userRepository.findById(taskCreate.assignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + taskCreate.assignedUserId()));
            task.setAssignedUser(user);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskRecord getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return convertToDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByCategoryId(Long categoryId) {
        return taskRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByUserId(Long userId) {
        return taskRepository.findByAssignedUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    public TaskRecord changeTaskStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTasksByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    private TaskRecord convertToDTO(Task task) {
        boolean isOverdue = task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()) && task.getStatus() != TaskStatus.DONE;

        return new TaskRecord(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCategory() != null ? task.getCategory().getName() : null,
                task.getAssignedUser() != null ? task.getAssignedUser().getUsername() : null,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getDueDate(),
                isOverdue
        );
    }
}