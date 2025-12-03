package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.Role;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Category;
import br.com.rafaelvieira.taskmanagement.domain.model.PomodoroSession;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.TaskValidationException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.repository.PomodoroSessionRepository;
import br.com.rafaelvieira.taskmanagement.repository.SquadMemberRepository;
import br.com.rafaelvieira.taskmanagement.repository.SquadRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.repository.UserRepository;
import br.com.rafaelvieira.taskmanagement.service.GamificationService;
import br.com.rafaelvieira.taskmanagement.service.GamificationWebSocketService;
import br.com.rafaelvieira.taskmanagement.service.MonthlyBadgeService;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private static final String EXCEPTION_TASK_ID = "Task not found with id: ";

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SquadRepository squadRepository;
    private final SquadMemberRepository squadMemberRepository;
    private final UserService userService;
    private final PomodoroSessionRepository pomodoroSessionRepository;
    private final GamificationService gamificationService;
    private final GamificationWebSocketService webSocketService;
    private final MonthlyBadgeService monthlyBadgeService;
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
                task.getExtensionJustification(),
                task.getSquad() != null ? task.getSquad().getId() : null,
                task.getSquad() != null && task.getSquad().getLead() != null
                        ? task.getSquad().getLead().getId()
                        : null);
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
        // Se o record tiver squadId, usar o método com squad
        if (taskCreate.squadId() != null) {
            return createTask(taskCreate, taskCreate.squadId());
        }

        User currentUser = userService.getCurrentUser();

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
                        .createdBy(currentUser)
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
    public TaskRecord createTask(TaskCreateRecord taskCreate, Long squadId) {
        User currentUser = userService.getCurrentUser();

        // Validar que apenas LEAD/ADMIN podem criar tasks de squad
        if (squadId != null && currentUser.getRole() == Role.MEMBER) {
            throw new UnauthorizedException("Only LEAD or ADMIN can create squad tasks");
        }

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
                        .createdBy(currentUser)
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

        // Atribuir squad se fornecido
        if (squadId != null) {
            Squad squad =
                    squadRepository
                            .findById(squadId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Squad not found with id: " + squadId));

            if (!squad.getLead().getId().equals(currentUser.getId())
                    && currentUser.getRole() != Role.ADMIN) {
                throw new UnauthorizedException(
                        "Only the squad lead or ADMIN can create tasks for this squad");
            }

            task.setSquad(squad);
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

        log.info(
                "Task '{}' created by {} for squad: {}",
                savedTask.getTitle(),
                currentUser.getUsername(),
                squadId != null ? squadId : "personal");

        return convertTo(savedTask);
    }

    @Override
    @Transactional
    public TaskRecord updateTask(Long id, TaskCreateRecord taskCreate) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        User currentUser = userService.getCurrentUser();

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

        // Processar squadId (apenas ADMIN/LEAD podem alterar)
        if (currentUser.getRole() != Role.MEMBER) {
            if (taskCreate.squadId() != null) {
                Squad squad =
                        squadRepository
                                .findById(taskCreate.squadId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Squad not found with id: "
                                                                + taskCreate.squadId()));
                task.setSquad(squad);
            } else {
                task.setSquad(null);
            }
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
    @Transactional
    public TaskRecord updateTask(Long id, TaskCreateRecord taskCreate, User currentUser) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        // Verificar permissão de modificação
        if (!canUserModifyTask(currentUser, task)) {
            throw new UnauthorizedException("You don't have permission to modify this task");
        }

        // MEMBERs não podem alterar dueDate em tasks de squad
        boolean isDueDateChange = !Objects.equals(taskCreate.dueDate(), task.getDueDate());
        if (task.getSquad() != null && currentUser.getRole() == Role.MEMBER && isDueDateChange) {
            throw new UnauthorizedException("Members cannot change the due date of squad tasks");
        }

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

        // Processar squadId (apenas ADMIN/LEAD podem alterar)
        if (currentUser.getRole() != Role.MEMBER) {
            if (taskCreate.squadId() != null) {
                Squad squad =
                        squadRepository
                                .findById(taskCreate.squadId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Squad not found with id: "
                                                                + taskCreate.squadId()));
                task.setSquad(squad);
            } else {
                task.setSquad(null);
            }
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
    public List<TaskRecord> getVisibleTasks(User currentUser) {
        List<Task> tasks;

        switch (currentUser.getRole()) {
            case ADMIN:
                // Admin vê todas as tasks
                tasks = taskRepository.findAll();
                break;
            case LEAD:
                // Lead vê tasks pessoais + tasks de squads que lidera
                tasks = taskRepository.findVisibleTasksForLead(currentUser);
                break;
            case MEMBER:
            default:
                // Member vê tasks pessoais + tasks de squads onde é membro
                tasks = taskRepository.findVisibleTasksForMember(currentUser);
                break;
        }

        return tasks.stream().map(TaskServiceImpl::convertTo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRecord> getTasksBySquadId(Long squadId) {
        return taskRepository.findBySquadId(squadId).stream()
                .map(TaskServiceImpl::convertTo)
                .toList();
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
    public void deleteTask(Long id, User currentUser) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        if (!canUserDeleteTask(currentUser, task)) {
            throw new UnauthorizedException("You don't have permission to delete this task");
        }

        taskRepository.deleteById(id);
        log.info("Task {} deleted by user {}", id, currentUser.getUsername());
    }

    @Override
    public boolean canUserModifyTask(User user, Task task) {
        if (user.getRole() == Role.ADMIN) {
            return true; // Admin pode modificar qualquer task
        }

        if (task.getSquad() == null) {
            // Task pessoal - apenas o usuário atribuído pode modificar
            return task.getAssignedUser() != null
                    && task.getAssignedUser().getId().equals(user.getId());
        }

        // Task de squad
        if (user.getRole() == Role.LEAD) {
            // Lead pode modificar tasks de squads que lidera
            return task.getSquad().getLead().getId().equals(user.getId());
        }

        // MEMBER pode modificar tasks de squad onde é membro (com restrições)
        return squadMemberRepository.existsBySquadAndUser(task.getSquad(), user);
    }

    @Override
    public boolean canUserDeleteTask(User user, Task task) {
        if (user.getRole() == Role.ADMIN) {
            return true; // Admin pode deletar qualquer task
        }

        if (task.getSquad() == null) {
            // Task pessoal - apenas o usuário atribuído ou criador pode deletar
            boolean isAssigned =
                    task.getAssignedUser() != null
                            && task.getAssignedUser().getId().equals(user.getId());
            boolean isCreator =
                    task.getCreatedBy() != null && task.getCreatedBy().getId().equals(user.getId());
            return isAssigned || isCreator;
        }

        // Task de squad - apenas LEAD do squad pode deletar
        if (user.getRole() == Role.LEAD) {
            return task.getSquad().getLead().getId().equals(user.getId());
        }

        // MEMBER não pode deletar tasks de squad
        return false;
    }

    @Override
    public boolean canUserControlTimer(User user, Task task) {
        if (user.getRole() == Role.ADMIN) {
            return true; // Admin pode controlar timer de qualquer task
        }

        if (task.getSquad() == null) {
            // Task pessoal - apenas o usuário atribuído pode controlar o timer
            return task.getAssignedUser() != null
                    && task.getAssignedUser().getId().equals(user.getId());
        }

        // Task de squad - apenas LEAD do squad pode controlar timer
        if (user.getRole() == Role.LEAD) {
            return task.getSquad().getLead().getId().equals(user.getId());
        }

        // MEMBER não pode controlar timer de tasks de squad
        return false;
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

                return convertTo(task);
            }
            System.out.println("✅ Task " + id + " can start - proceeding...");
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
                task.setCompletedAt(LocalDateTime.now());

                // Trigger gamification for squad tasks
                if (task.getSquad() != null) {
                    try {
                        gamificationService.registerTaskCompletion(task);
                    } catch (ResourceNotFoundException e) {
                        System.err.println(
                                "Error registering task completion for gamification: "
                                        + e.getMessage());
                    }
                }

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
    public TaskRecord changeTaskStatus(Long id, TaskStatus newStatus, User currentUser) {
        Task task =
                taskRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(EXCEPTION_TASK_ID + id));

        // Verificar se o usuário pode controlar o timer (para status que afetam o
        // timer)
        boolean isTimerRelatedStatus =
                newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.IN_PAUSE;

        if (isTimerRelatedStatus && !canUserControlTimer(currentUser, task)) {
            throw new UnauthorizedException(
                    "You don't have permission to control the timer on this squad task");
        }

        // Delegar para o método original após verificação
        return changeTaskStatusInternal(task, newStatus);
    }

    private TaskRecord changeTaskStatusInternal(Task task, TaskStatus newStatus) {
        log.debug(
                "changeTaskStatus called - ID: {}, Current Status: {}, New Status: {}, ExecTime:"
                        + " {}, Pomodoro: {}",
                task.getId(),
                task.getStatus(),
                newStatus,
                task.getExecutionTimeMinutes(),
                task.getPomodoroMinutes());

        if (newStatus == TaskStatus.IN_PROGRESS) {
            if (!canStart(task)) {
                log.warn(
                        "Cannot start task ID {}: executionTimeMinutes={}, pomodoroMinutes={} ->"
                                + " keeping status {}",
                        task.getId(),
                        task.getExecutionTimeMinutes(),
                        task.getPomodoroMinutes(),
                        task.getStatus());
                return convertTo(task);
            }

            ensureAssignedToCurrentUserIfStarting(task);

            boolean isResumed =
                    task.getStatus() == TaskStatus.IN_PAUSE
                            || task.getStatus() == TaskStatus.PENDING;

            if (task.getStatus() != TaskStatus.IN_PROGRESS) {
                startMainTimer(task);
                maybeStartPomodoro(task);
            }
            finishActivePomodoros(task);
            task.setStatus(TaskStatus.IN_PROGRESS);

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
                task.setCompletedAt(LocalDateTime.now());

                // Trigger gamification for squad tasks
                if (task.getSquad() != null && task.getAssignedUser() != null) {
                    try {
                        gamificationService.registerTaskCompletion(task);
                        // Registrar para badge mensal
                        monthlyBadgeService.registerSquadTaskCompletion(task.getAssignedUser());
                    } catch (ResourceNotFoundException e) {
                        log.error(
                                "Error registering task completion for gamification: {}",
                                e.getMessage());
                    }
                }

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
    @Transactional(readOnly = true)
    public Page<@NotNull TaskRecord> searchTasks(
            br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm filter,
            Pageable pageable,
            User currentUser) {
        // Obter tasks visíveis para o usuário baseado em seu role
        List<Task> visibleTasks;

        switch (currentUser.getRole()) {
            case ADMIN:
                visibleTasks = taskRepository.findAll();
                break;
            case LEAD:
                visibleTasks = taskRepository.findVisibleTasksForLead(currentUser);
                break;
            case MEMBER:
            default:
                visibleTasks = taskRepository.findVisibleTasksForMember(currentUser);
                break;
        }

        // Aplicar filtros adicionais
        var filtered =
                visibleTasks.stream()
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

        Integer currentExtra = task.getExtraTimeMinutes() != null ? task.getExtraTimeMinutes() : 0;
        task.setExtraTimeMinutes(currentExtra + extension.extraTimeMinutes());

        Integer currentExecution =
                task.getExecutionTimeMinutes() != null ? task.getExecutionTimeMinutes() : 0;
        task.setExecutionTimeMinutes(currentExecution + extension.extraTimeMinutes());

        if (extension.justification() != null && !extension.justification().isEmpty()) {
            String currentJustification = task.getExtensionJustification();
            if (currentJustification != null && !currentJustification.isEmpty()) {
                task.setExtensionJustification(
                        currentJustification + "\n---\n" + extension.justification());
            } else {
                task.setExtensionJustification(extension.justification());
            }
        }

        if (extension.scheduledStartAt() != null) {
            task.setScheduledStartAt(extension.scheduledStartAt());
        }
        if (extension.dueDate() != null) {
            task.setDueDate(extension.dueDate());
        }

        if (task.getStatus() == TaskStatus.OVERDUE || task.getStatus() == TaskStatus.PENDING) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledStart = task.getScheduledStartAt();

            if (scheduledStart != null && scheduledStart.isAfter(now)) {
                task.setStatus(TaskStatus.TODO);
                task.setPomodoroUntil(null);
                task.setMainStartedAt(null);
            } else {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setPomodoroUntil(null);
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

    @Override
    @Transactional
    public TaskRecord assignTaskToCurrentUser(Long taskId, User currentUser) {
        Task task =
                taskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(EXCEPTION_TASK_ID + taskId));

        // Se já possui responsável
        if (task.getAssignedUser() != null) {
            if (task.getAssignedUser().getId().equals(currentUser.getId())) {
                log.debug(
                        "Task {} already assigned to current user {}",
                        taskId,
                        currentUser.getUsername());
                return convertTo(task); // nada a fazer
            } else {
                throw new TaskValidationException("A tarefa já possui responsável.");
            }
        }

        // Verificar se a task pertence a um squad do qual o usuário é membro
        if (task.getSquad() != null) {
            boolean isMember =
                    squadMemberRepository.existsBySquadAndUser(task.getSquad(), currentUser);
            boolean isLead = task.getSquad().getLead().getId().equals(currentUser.getId());

            if (!isMember && !isLead && currentUser.getRole() != Role.ADMIN) {
                throw new UnauthorizedException("You are not a member of this task's squad");
            }
        }

        // Atribuir a task ao usuário atual
        task.setAssignedUser(currentUser);
        Task savedTask = taskRepository.save(task);

        log.info("Task {} assigned to user {}", taskId, currentUser.getUsername());

        eventPublisher.publishEvent(
                new br.com.rafaelvieira.taskmanagement.event.TaskEvent(
                        this,
                        savedTask,
                        br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType
                                .TASK_UPDATED,
                        currentUser.getFullName() + " assumiu a tarefa: " + savedTask.getTitle()));

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
