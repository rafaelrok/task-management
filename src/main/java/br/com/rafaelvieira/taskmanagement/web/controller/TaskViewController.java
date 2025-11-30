package br.com.rafaelvieira.taskmanagement.web.controller;

import static java.util.Collections.emptyList;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.TaskValidationException;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm;
import br.com.rafaelvieira.taskmanagement.web.dto.TaskForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class TaskViewController {

    private static final Logger LOG = LoggerFactory.getLogger(TaskViewController.class);
    private static final String REDIRECT_TASKS = "redirect:/tasks";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private final TaskService taskService;
    private final CategoryRepository categoryRepository;

    public TaskViewController(TaskService taskService, CategoryRepository categoryRepository) {
        this.taskService = taskService;
        this.categoryRepository = categoryRepository;
    }

    @ModelAttribute("allStatuses")
    public TaskStatus[] allStatuses() {
        return TaskStatus.values();
    }

    @ModelAttribute("allPriorities")
    public Priority[] allPriorities() {
        return Priority.values();
    }

    @ModelAttribute("allCategories")
    public java.util.List<br.com.rafaelvieira.taskmanagement.domain.model.Category> allCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            model.addAttribute("totalTodo", taskService.countTasksByStatus(TaskStatus.TODO));
            model.addAttribute(
                    "totalInProgress", taskService.countTasksByStatus(TaskStatus.IN_PROGRESS));
            model.addAttribute("totalDone", taskService.countTasksByStatus(TaskStatus.DONE));
            model.addAttribute(
                    "totalCancelled", taskService.countTasksByStatus(TaskStatus.CANCELLED));

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDate today = now.toLocalDate();

            java.util.List<TaskRecord> todoTasks = taskService.getTasksByStatus(TaskStatus.TODO);
            java.util.List<TaskRecord> overdue = new java.util.ArrayList<>(taskService.getOverdueTasks());

            for (TaskRecord t : todoTasks) {
                if (t.scheduledStartAt() != null && t.scheduledStartAt().isBefore(now)) {
                    if (overdue.stream().noneMatch(o -> o.id().equals(t.id()))) {
                        overdue.add(t);
                    }
                }
            }
            model.addAttribute("overdueTasks", overdue);

            java.util.List<TaskRecord> dueToday = new java.util.ArrayList<>();
            java.util.List<TaskRecord> allStatuses = taskService.getAllTasks();
            for (TaskRecord t : allStatuses) {
                boolean dueDateToday = t.dueDate() != null && t.dueDate().toLocalDate().equals(today);
                boolean scheduledToday = t.scheduledStartAt() != null
                        && t.scheduledStartAt().toLocalDate().equals(today);
                if (dueDateToday || scheduledToday) {
                    dueToday.add(t);
                }
            }
            dueToday.sort(
                    (a, b) -> {
                        java.time.LocalDateTime aKey = a.scheduledStartAt() != null ? a.scheduledStartAt()
                                : a.dueDate();
                        java.time.LocalDateTime bKey = b.scheduledStartAt() != null ? b.scheduledStartAt()
                                : b.dueDate();
                        if (aKey == null && bKey == null) {
                            return 0;
                        }
                        if (aKey == null) {
                            return 1;
                        }
                        if (bKey == null) {
                            return -1;
                        }
                        return aKey.compareTo(bKey);
                    });
            model.addAttribute("dueToday", dueToday);

            model.addAttribute("countLow", taskService.countTasksByPriority(Priority.LOW));
            model.addAttribute("countMedium", taskService.countTasksByPriority(Priority.MEDIUM));
            model.addAttribute("countHigh", taskService.countTasksByPriority(Priority.HIGH));
            model.addAttribute("countUrgent", taskService.countTasksByPriority(Priority.URGENT));

            populateActiveTasks(model, todoTasks, now);
            populateScheduledTasks(model, todoTasks, now);

        } catch (ResourceNotFoundException e) {
            LOG.error("Error loading dashboard", e);
            model.addAttribute("overdueTasks", emptyList());
            model.addAttribute("dueToday", emptyList());
            model.addAttribute("activeTasks", emptyList());
            model.addAttribute("scheduledTasks", emptyList());
            model.addAttribute("totalTodo", 0L);
            model.addAttribute("totalInProgress", 0L);
            model.addAttribute("totalDone", 0L);
            model.addAttribute("totalCancelled", 0L);
            model.addAttribute("countLow", 0L);
            model.addAttribute("countMedium", 0L);
            model.addAttribute("countHigh", 0L);
            model.addAttribute("countUrgent", 0L);
        }

        return "dashboard";
    }

    @GetMapping("/dashboard/active-tasks")
    public String getActiveTasksFragment(Model model) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TaskRecord> todoTasks = taskService.getTasksByStatus(TaskStatus.TODO);
            populateActiveTasks(model, todoTasks, now);
        } catch (TaskValidationException e) {
            LOG.error("Error loading active tasks fragment", e);
            model.addAttribute("activeTasks", emptyList());
        }
        return "fragments/dashboard-active :: active-tasks";
    }

    @GetMapping("/dashboard/scheduled-tasks")
    public String getScheduledTasksFragment(Model model) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TaskRecord> todoTasks = taskService.getTasksByStatus(TaskStatus.TODO);
            populateScheduledTasks(model, todoTasks, now);
        } catch (TaskValidationException e) {
            LOG.error("Error loading scheduled tasks fragment", e);
            model.addAttribute("scheduledTasks", emptyList());
        }
        return "fragments/dashboard-scheduled :: scheduled-tasks";
    }

    @GetMapping("/dashboard/overdue-tasks")
    public String getOverdueTasksFragment(Model model) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TaskRecord> todoTasks = taskService.getTasksByStatus(TaskStatus.TODO);
            List<TaskRecord> overdue = new ArrayList<>(taskService.getOverdueTasks());

            for (TaskRecord t : todoTasks) {
                if (t.scheduledStartAt() != null && t.scheduledStartAt().isBefore(now)) {
                    if (overdue.stream().noneMatch(o -> o.id().equals(t.id()))) {
                        overdue.add(t);
                    }
                }
            }
            model.addAttribute("overdueTasks", overdue);
        } catch (TaskValidationException e) {
            LOG.error("Error loading overdue tasks fragment", e);
            model.addAttribute("overdueTasks", emptyList());
        }
        return "fragments/dashboard-overdue :: overdue-tasks";
    }

    @GetMapping("/dashboard/due-today-tasks")
    public String getDueTodayTasksFragment(Model model) {
        try {
            LocalDate today = LocalDate.now();
            List<TaskRecord> dueToday = new ArrayList<>();
            List<TaskRecord> allStatuses = taskService.getAllTasks();

            for (TaskRecord t : allStatuses) {
                boolean dueDateToday = t.dueDate() != null && t.dueDate().toLocalDate().equals(today);
                boolean scheduledToday = t.scheduledStartAt() != null
                        && t.scheduledStartAt().toLocalDate().equals(today);
                if (dueDateToday || scheduledToday) {
                    dueToday.add(t);
                }
            }
            dueToday.sort(
                    (a, b) -> {
                        LocalDateTime aKey = a.scheduledStartAt() != null ? a.scheduledStartAt() : a.dueDate();
                        LocalDateTime bKey = b.scheduledStartAt() != null ? b.scheduledStartAt() : b.dueDate();
                        if (aKey == null && bKey == null) {
                            return 0;
                        }
                        if (aKey == null) {
                            return 1;
                        }
                        if (bKey == null) {
                            return -1;
                        }
                        return aKey.compareTo(bKey);
                    });
            model.addAttribute("dueToday", dueToday);
        } catch (TaskValidationException e) {
            LOG.error("Error loading due today tasks fragment", e);
            model.addAttribute("dueToday", emptyList());
        }
        return "fragments/dashboard-due-today :: due-today-tasks";
    }

    private void populateActiveTasks(
            Model model, java.util.List<TaskRecord> todoTasks, java.time.LocalDateTime now) {
        java.util.List<TaskRecord> active = new java.util.ArrayList<>();
        active.addAll(taskService.getTasksByStatus(TaskStatus.IN_PROGRESS));
        active.addAll(taskService.getTasksByStatus(TaskStatus.IN_PAUSE));
        // Incluir tarefas PENDING (tempo finalizado, aguardando ação)
        active.addAll(taskService.getTasksByStatus(TaskStatus.PENDING));
        // Incluir tarefas OVERDUE (atrasadas)
        active.addAll(taskService.getTasksByStatus(TaskStatus.OVERDUE));
        for (TaskRecord t : todoTasks) {
            if (t.scheduledStartAt() != null
                    && !t.scheduledStartAt().isAfter(now)
                    && t.executionTimeMinutes() != null
                    && t.pomodoroMinutes() != null) {
                active.add(t); // pending start
            }
        }
        active.sort(
                (a, b) -> {
                    java.time.LocalDateTime ua = a.updatedAt();
                    java.time.LocalDateTime ub = b.updatedAt();
                    if (ua == null && ub == null) {
                        return 0;
                    }
                    if (ua == null) {
                        return 1;
                    }
                    if (ub == null) {
                        return -1;
                    }
                    return ub.compareTo(ua);
                });
        model.addAttribute("activeTasks", active);
    }

    private void populateScheduledTasks(
            Model model, java.util.List<TaskRecord> todoTasks, java.time.LocalDateTime now) {
        java.util.List<TaskRecord> scheduled = new java.util.ArrayList<>();
        for (TaskRecord t : todoTasks) {
            if (t.scheduledStartAt() != null && t.scheduledStartAt().isAfter(now)) {
                scheduled.add(t);
            }
        }
        scheduled.sort(
                (a, b) -> {
                    if (a.scheduledStartAt() == null && b.scheduledStartAt() == null) {
                        return 0;
                    }
                    if (a.scheduledStartAt() == null) {
                        return 1;
                    }
                    if (b.scheduledStartAt() == null) {
                        return -1;
                    }
                    return a.scheduledStartAt().compareTo(b.scheduledStartAt());
                });
        model.addAttribute("scheduledTasks", scheduled);
    }

    @GetMapping("/tasks")
    public String listTasks(
            @ModelAttribute("filter") TaskFilterForm filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            HttpServletRequest request,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<@NotNull TaskRecord> tasksPage = taskService.searchTasks(filter, pageable);

        // Sort tasks: IN_PROGRESS and IN_PAUSE first, then by updatedAt desc
        java.util.List<TaskRecord> sortedTasks = tasksPage.getContent().stream()
                .sorted(
                        (t1, t2) -> {
                            // Priority 1: IN_PROGRESS status
                            boolean t1InProgress = t1.status() == TaskStatus.IN_PROGRESS;
                            boolean t2InProgress = t2.status() == TaskStatus.IN_PROGRESS;
                            if (t1InProgress != t2InProgress) {
                                return t1InProgress ? -1 : 1;
                            }

                            // Priority 2: IN_PAUSE status
                            boolean t1InPause = t1.status() == TaskStatus.IN_PAUSE;
                            boolean t2InPause = t2.status() == TaskStatus.IN_PAUSE;
                            if (t1InPause != t2InPause) {
                                return t1InPause ? -1 : 1;
                            }

                            // Priority 3: OVERDUE status
                            boolean t1Overdue = t1.overdue();
                            boolean t2Overdue = t2.overdue();
                            if (t1Overdue != t2Overdue) {
                                return t1Overdue ? -1 : 1;
                            }

                            // Priority 4: Most recently updated
                            if (t1.updatedAt() != null && t2.updatedAt() != null) {
                                return t2.updatedAt().compareTo(t1.updatedAt());
                            }
                            return 0;
                        })
                .toList();

        Page<@NotNull TaskRecord> sortedPage = new org.springframework.data.domain.PageImpl<>(
                sortedTasks, pageable, tasksPage.getTotalElements());

        model.addAttribute("tasksPage", sortedPage);
        model.addAttribute("filter", filter);

        // If AJAX request, return only the fragment (partial HTML)
        String requestedWith = request.getHeader("X-Requested-With");
        boolean ajaxParam = request.getParameter("ajax") != null;
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith) || ajaxParam) {
            return "fragments/task-table :: fragment";
        }
        return "tasks/list";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        TaskForm form = new TaskForm();
        form.setStatus(TaskStatus.TODO);
        form.setPriority(Priority.MEDIUM);
        form.setPomodoroBreakMinutes(5);
        model.addAttribute("taskForm", form);
        return "tasks/create";
    }

    @PostMapping("/tasks")
    public String createTask(
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskForm", form);
            return "tasks/create";
        }

        form.setStatus(TaskStatus.TODO);

        TaskCreateRecord taskCreateRecord = new TaskCreateRecord(
                form.getTitle(),
                form.getDescription(),
                TaskStatus.TODO,
                form.getPriority(),
                form.getCategoryId(),
                form.getAssignedUserId(),
                form.getDueDate(),
                form.getScheduledStartAt(),
                form.getPomodoroMinutes(),
                form.getPomodoroBreakMinutes(),
                form.getExecutionTimeMinutes());
        taskService.createTask(taskCreateRecord);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tarefa criada com sucesso!");
        return REDIRECT_TASKS;
    }

    @GetMapping("/tasks/{id}")
    public String editTaskForm(@PathVariable("id") Long id, Model model) {
        var task = taskService.getTaskById(id);
        TaskForm form = new TaskForm();
        form.setId(task.id());
        form.setTitle(task.title());
        form.setDescription(task.description());
        form.setStatus(task.status());
        form.setPriority(task.priority());
        form.setDueDate(task.dueDate());
        form.setCategoryId(task.categoryId());
        form.setAssignedUserId(task.assignedUserId());
        form.setScheduledStartAt(task.scheduledStartAt());
        form.setPomodoroMinutes(task.pomodoroMinutes());
        form.setPomodoroBreakMinutes(task.pomodoroBreakMinutes());
        form.setExecutionTimeMinutes(task.executionTimeMinutes());
        model.addAttribute("taskForm", form);
        model.addAttribute("task", task);
        return "tasks/edit";
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            var task = taskService.getTaskById(id);
            model.addAttribute("taskForm", form);
            model.addAttribute("task", task);
            return "tasks/edit";
        }
        try {
            TaskCreateRecord taskCreateRecord = new TaskCreateRecord(
                    form.getTitle(),
                    form.getDescription(),
                    form.getStatus(),
                    form.getPriority(),
                    form.getCategoryId(),
                    form.getAssignedUserId(),
                    form.getDueDate(),
                    form.getScheduledStartAt(),
                    form.getPomodoroMinutes(),
                    form.getPomodoroBreakMinutes(),
                    form.getExecutionTimeMinutes());
            taskService.updateTask(id, taskCreateRecord);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tarefa atualizada com sucesso!");
        } catch (TaskValidationException e) {
            System.err.println(
                    "❌ Error updating task "
                            + id
                            + " to status "
                            + form.getStatus()
                            + ": "
                            + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Erro ao atualizar tarefa: " + e.getMessage());
            return "redirect:/tasks/" + id;
        }
        return REDIRECT_TASKS;
    }

    @PostMapping("/tasks/{id}/status")
    public String changeStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") TaskStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            taskService.changeTaskStatus(id, status);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Status da tarefa atualizado!");
        } catch (br.com.rafaelvieira.taskmanagement.exception.TaskValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tasks/" + id;
        }
        return REDIRECT_TASKS;
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tarefa excluída com sucesso!");
        return REDIRECT_TASKS;
    }
}
