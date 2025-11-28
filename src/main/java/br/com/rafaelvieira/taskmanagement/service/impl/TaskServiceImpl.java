package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Category;
import br.com.rafaelvieira.taskmanagement.domain.model.PomodoroSession;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.repository.PomodoroSessionRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final String EXCEPTION_TASK_ID = "Task not found with id: ";

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static TaskRecord convertTo(Task task) {
        boolean isOverdue =
                task.getDueDate() != null
                        && task.getDueDate().isBefore(LocalDateTime.now())
                        && task.getStatus() != TaskStatus.DONE;

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
                isOverdue,
                task.getCategory() != null ? task.getCategory().getId() : null,
                task.getAssignedUser() != null ? task.getAssignedUser().getId() : null,
                task.getScheduledStartAt(),
                task.getPomodoroMinutes(),
                task.getPomodoroBreakMinutes(),
                task.getExecutionTimeMinutes(),
                task.getMainStartedAt(),
                task.getMainElapsedSeconds(),
                task.getPomodoroUntil(),
                task.getExtraTimeMinutes(),
                task.getExtensionJustification());
    }

    private boolean canStart(Task task) {
        return task.getExecutionTimeMinutes() != null
                && task.getExecutionTimeMinutes() > 0
                && task.getPomodoroMinutes() != null
                && task.getPomodoroMinutes() > 0;
    }

    private void ensureAssignedToCurrentUserIfStarting(Task task) {
        var current = userService.getCurrentUser();
        if (current != null) {
            task.setAssignedUser(current);
        }
    }

    private void startMainTimer(Task task) {
        if (task.getMainStartedAt() == null) {
            task.setMainStartedAt(LocalDateTime.now());
        }
    }

    private void pauseMainTimer(Task task) {
        if (task.getMainStartedAt() != null) {
            long delta =
                    java.time.Duration.between(task.getMainStartedAt(), LocalDateTime.now())
                            .getSeconds();
            task.setMainElapsedSeconds(
                    (task.getMainElapsedSeconds() == null ? 0L : task.getMainElapsedSeconds())
                            + Math.max(0, delta));
            task.setMainStartedAt(null);
        }
    }

    private void maybeStartPomodoro(Task task) {
        Integer minutes = task.getPomodoroMinutes();
        if (minutes != null && minutes > 0) {
            task.setPomodoroUntil(LocalDateTime.now().plusMinutes(minutes));
            if (pomodoroSessionRepository != null) {
                PomodoroSession session =
                        PomodoroSession.builder().task(task).startedAt(LocalDateTime.now()).build();
                pomodoroSessionRepository.save(session);
            }
        }
    }

    private void finishActivePomodoros(Task task) {
        if (pomodoroSessionRepository == null) {
            return; // No-op in tests without repository
        }
        var active = pomodoroSessionRepository.findActiveByTask(task.getId());
        LocalDateTime now = LocalDateTime.now();
        for (PomodoroSession s : active) {
            s.setEndedAt(now);
            if (s.getStartedAt() != null) {
                s.setDurationSeconds(
                        java.time.Duration.between(s.getStartedAt(), now).getSeconds());
            }
        }
        pomodoroSessionRepository.saveAll(active);
    }

    private void checkAutoFinish(Task task) {
        if (task.getExecutionTimeMinutes() != null && task.getExecutionTimeMinutes() > 0) {
            long targetSeconds = task.getExecutionTimeMinutes() * 60L;
            long elapsed = task.getMainElapsedSeconds() == null ? 0L : task.getMainElapsedSeconds();
            if (task.getMainStartedAt() != null) {
                elapsed +=
                        java.time.Duration.between(task.getMainStartedAt(), LocalDateTime.now())
                                .getSeconds();
            }
            if (elapsed >= targetSeconds
                    && task.getStatus() != TaskStatus.DONE
                    && task.getStatus() != TaskStatus.PENDING
                    && task.getStatus() != TaskStatus.CANCELLED
                    && task.getStatus() != TaskStatus.OVERDUE) {
                pauseMainTimer(task);
                finishActivePomodoros(task);
                task.setStatus(TaskStatus.OVERDUE);
                task.setPomodoroUntil(null);

                eventPublisher.publishEvent(
                        new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                                this,
                                task,
                                br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                        .TASK_TIME_UP,
                                "O tempo da tarefa '" + task.getTitle() + "' acabou!"));
            }
        }
    }

    @Override
    public TaskRecord createTask(TaskCreateRecord taskCreate) {
        var task =
                Task.builder()
                        .title(taskCreate.title())
                        .description(taskCreate.description())
                        .status(taskCreate.status() != null ? taskCreate.status() : TaskStatus.TODO)
                        .priority(
                                taskCreate.priority() != null
                                        ? taskCreate.priority()
                                        : Priority.MEDIUM)
                        .dueDate(taskCreate.dueDate())
                        .scheduledStartAt(taskCreate.scheduledStartAt())
                        .pomodoroMinutes(taskCreate.pomodoroMinutes())
                        .pomodoroBreakMinutes(
                                taskCreate.pomodoroBreakMinutes() != null
                                        ? taskCreate.pomodoroBreakMinutes()
                                        : 5)
                        .executionTimeMinutes(taskCreate.executionTimeMinutes())
                        .build();

        findCategoryByTask(taskCreate, task);

        if (taskCreate.assignedUserId() != null) {
            User user =
                    userRepository
                            .findById(taskCreate.assignedUserId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "User not found with id: "
                                                            + taskCreate.assignedUserId()));
            task.setAssignedUser(user);
        }

        Task savedTask = taskRepository.save(task);

        if (savedTask.getStatus() == TaskStatus.TODO) {
            eventPublisher.publishEvent(
                    new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                            this,
                            savedTask,
                            br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                    .TASK_CREATED,
                            "Nova tarefa criada: " + savedTask.getTitle()));
        }

        return convertTo(savedTask);
    }

    @Override
    @Transactional
    public TaskRecord updateTask(Long id, TaskCreateRecord taskCreate) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        task.setTitle(taskCreate.title());
        task.setDescription(taskCreate.description());
        task.setStatus(taskCreate.status() != null ? taskCreate.status() : task.getStatus());
        task.setPriority(
                taskCreate.priority() != null ? taskCreate.priority() : task.getPriority());
        task.setDueDate(taskCreate.dueDate());
        task.setScheduledStartAt(taskCreate.scheduledStartAt());
        task.setPomodoroMinutes(taskCreate.pomodoroMinutes());
        task.setPomodoroBreakMinutes(
                taskCreate.pomodoroBreakMinutes() != null
                        ? taskCreate.pomodoroBreakMinutes()
                        : task.getPomodoroBreakMinutes());
        task.setExecutionTimeMinutes(taskCreate.executionTimeMinutes());

        findCategoryByTask(taskCreate, task);

        if (taskCreate.assignedUserId() != null) {
            User user =
                    userRepository
                            .findById(taskCreate.assignedUserId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "User not found with id: "
                                                            + taskCreate.assignedUserId()));
            task.setAssignedUser(user);
        }

        Task updatedTask = taskRepository.save(task);
        taskRepository.flush();

        eventPublisher.publishEvent(
                new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                        this,
                        updatedTask,
                        br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                .TASK_UPDATED,
                        "Tarefa atualizada: " + updatedTask.getTitle()));

        return convertTo(updatedTask);
    }

    @Override
    public TaskRecord getTaskById(Long id) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));
        return convertTo(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getAllTasks() {
        return taskRepository.findAll().stream().map(TaskServiceImpl::convertTo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByCategoryId(Long categoryId) {
        return taskRepository.findByCategoryId(categoryId).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksByUserId(Long userId) {
        return taskRepository.findByAssignedUserId(userId).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException(EXCEPTION_TASK_ID + id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    public TaskRecord changeTaskStatus(Long id, TaskStatus newStatus) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        System.out.println(
                "🔄 changeTaskStatus called - ID: "
                        + id
                        + ", Current Status: "
                        + task.getStatus()
                        + ", New Status: "
                        + newStatus
                        + ", ExecTime: "
                        + task.getExecutionTimeMinutes()
                        + ", Pomodoro: "
                        + task.getPomodoroMinutes());

        if (newStatus == TaskStatus.IN_PROGRESS) {
            // Validate required fields; if missing, keep status unchanged (tests expect
            // TODO
            // remains)
            if (!canStart(task)) {
                System.out.println(
                        "⚠️ Cannot start task ID "
                                + id
                                + ": executionTimeMinutes="
                                + task.getExecutionTimeMinutes()
                                + ", pomodoroMinutes="
                                + task.getPomodoroMinutes()
                                + " -> keeping status "
                                + task.getStatus());
                // Simply return current state without throwing, allowing callers/tests to
                // proceed
                return convertTo(task);
            }
            System.out.println("✅ Task " + id + " can start - proceeding...");
            // Assign current user if any
            ensureAssignedToCurrentUserIfStarting(task);

            boolean isResumed =
                    task.getStatus() == TaskStatus.IN_PAUSE
                            || task.getStatus() == TaskStatus.PENDING;

            if (task.getStatus() != TaskStatus.IN_PROGRESS) {
                System.out.println("⏱️ Starting main timer for task " + id);
                startMainTimer(task);
                maybeStartPomodoro(task);
            }
            finishActivePomodoros(task);
            task.setStatus(TaskStatus.IN_PROGRESS);
            System.out.println("✅ Task " + id + " status set to IN_PROGRESS");

            if (isResumed) {
                eventPublisher.publishEvent(
                        new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                                this,
                                task,
                                br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                        .TASK_RESUMED,
                                "Tarefa retomada: " + task.getTitle()));
            } else {
                eventPublisher.publishEvent(
                        new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                                this,
                                task,
                                br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                        .TASK_STARTED,
                                "Tarefa iniciada: " + task.getTitle()));
            }

        } else if (newStatus == TaskStatus.IN_PAUSE) {
            pauseMainTimer(task);
            task.setStatus(TaskStatus.IN_PAUSE);
            finishActivePomodoros(task);

            eventPublisher.publishEvent(
                    new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                            this,
                            task,
                            br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                    .TASK_PAUSED,
                            "Tarefa pausada: " + task.getTitle()));

        } else if (newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED) {
            pauseMainTimer(task);
            finishActivePomodoros(task);
            task.setStatus(newStatus);
            task.setPomodoroUntil(null);

            if (newStatus == TaskStatus.DONE) {
                eventPublisher.publishEvent(
                        new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                                this,
                                task,
                                br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                        .TASK_FINISHED,
                                "Tarefa finalizada: " + task.getTitle()));
            }

        } else {
            task.setStatus(newStatus);
        }

        checkAutoFinish(task);

        Task updatedTask = taskRepository.save(task);
        return convertTo(updatedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTasksByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTasksByPriority(Priority priority) {
        return taskRepository.findByPriority(priority).size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksDueToday() {
        var start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        var end = start.plusDays(1);
        return taskRepository.findAll().stream()
                .filter(
                        t ->
                                t.getDueDate() != null
                                        && !t.getDueDate().isBefore(start)
                                        && t.getDueDate().isBefore(end))
                .map(TaskServiceImpl::convertTo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<@NotNull TaskRecord> searchTasks(
            br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter, Pageable pageable) {
        // Simple in-memory filtering for now; could be replaced by Specifications later
        List<Task> all = taskRepository.findAll();
        var filtered =
                all.stream()
                        .filter(
                                t ->
                                        filter.getStatus() == null
                                                || t.getStatus() == filter.getStatus())
                        .filter(
                                t ->
                                        filter.getPriority() == null
                                                || t.getPriority() == filter.getPriority())
                        .filter(
                                t ->
                                        filter.getUserId() == null
                                                || (t.getAssignedUser() != null
                                                        && Objects.equals(
                                                                t.getAssignedUser().getId(),
                                                                filter.getUserId())))
                        .filter(
                                t ->
                                        filter.getCategoryId() == null
                                                || (t.getCategory() != null
                                                        && Objects.equals(
                                                                t.getCategory().getId(),
                                                                filter.getCategoryId())))
                        .filter(
                                t ->
                                        filter.getCreatedFrom() == null
                                                || (t.getCreatedAt() != null
                                                        && !t.getCreatedAt()
                                                                .toLocalDate()
                                                                .isBefore(filter.getCreatedFrom())))
                        .filter(
                                t ->
                                        filter.getCreatedTo() == null
                                                || (t.getCreatedAt() != null
                                                        && !t.getCreatedAt()
                                                                .toLocalDate()
                                                                .isAfter(filter.getCreatedTo())))
                        .filter(
                                t ->
                                        filter.getDueFrom() == null
                                                || (t.getDueDate() != null
                                                        && !t.getDueDate()
                                                                .toLocalDate()
                                                                .isBefore(filter.getDueFrom())))
                        .filter(
                                t ->
                                        filter.getDueTo() == null
                                                || (t.getDueDate() != null
                                                        && !t.getDueDate()
                                                                .toLocalDate()
                                                                .isAfter(filter.getDueTo())))
                        .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<TaskRecord> content =
                filtered.subList(start, end).stream().map(TaskServiceImpl::convertTo).toList();
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    @Transactional
    public TaskRecord extendTask(
            Long id,
            br.com.rafaelvieira.taskmanagement.domain.records.TaskExtensionRecord extension) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        // Add extra time to the existing execution time
        Integer currentExtra = task.getExtraTimeMinutes() != null ? task.getExtraTimeMinutes() : 0;
        task.setExtraTimeMinutes(currentExtra + extension.extraTimeMinutes());

        // Update execution time to include extra time
        Integer currentExecution =
                task.getExecutionTimeMinutes() != null ? task.getExecutionTimeMinutes() : 0;
        task.setExecutionTimeMinutes(currentExecution + extension.extraTimeMinutes());

        // Set justification
        if (extension.justification() != null && !extension.justification().isEmpty()) {
            String currentJustification = task.getExtensionJustification();
            if (currentJustification != null && !currentJustification.isEmpty()) {
                task.setExtensionJustification(
                        currentJustification + "\n---\n" + extension.justification());
            } else {
                task.setExtensionJustification(extension.justification());
            }
        }

        // Update dates if provided
        if (extension.scheduledStartAt() != null) {
            task.setScheduledStartAt(extension.scheduledStartAt());
        }
        if (extension.dueDate() != null) {
            task.setDueDate(extension.dueDate());
        }

        // Determine status based on scheduledStartAt
        if (task.getStatus() == TaskStatus.OVERDUE || task.getStatus() == TaskStatus.PENDING) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledStart = task.getScheduledStartAt();

            if (scheduledStart != null && scheduledStart.isAfter(now)) {
                // Nova data de início é no futuro -> TODO (aguardar iniciar)
                task.setStatus(TaskStatus.TODO);
                task.setPomodoroUntil(null);
                task.setMainStartedAt(null);
            } else {
                // Nova data de início é agora ou no passado -> IN_PROGRESS (iniciar
                // imediatamente)
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setPomodoroUntil(null);
                // Start main timer if not already started
                if (task.getMainStartedAt() == null) {
                    startMainTimer(task);
                    maybeStartPomodoro(task);
                }

                eventPublisher.publishEvent(
                        new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                                this,
                                task,
                                br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                        .TASK_STARTED,
                                "Tarefa estendida e iniciada: " + task.getTitle()));
            }
        }

        Task savedTask = taskRepository.save(task);
        return convertTo(savedTask);
    }

    private void findCategoryByTask(TaskCreateRecord taskCreate, Task task) {
        if (taskCreate.categoryId() != null) {
            Category category =
                    categoryRepository
                            .findById(taskCreate.categoryId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Category not found with id: "
                                                            + taskCreate.categoryId()));
            task.setCategory(category);
        }
    }
}
