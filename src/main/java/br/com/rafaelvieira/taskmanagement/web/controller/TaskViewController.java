package br.com.rafaelvieira.taskmanagement.web.controller;

import static java.util.Collections.emptyList;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskCreateRecord;
import br.com.rafaelvieira.taskmanagement.domain.records.TaskRecord;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.repository.CategoryRepository;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.web.dto.TaskFilterForm;
import br.com.rafaelvieira.taskmanagement.web.dto.TaskForm;
import jakarta.validation.Valid;
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
    public java.util.List<br.com.rafaelvieira.taskmanagement.domain.model.Category>
            allCategories() {
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

            // Base lists
            java.util.List<TaskRecord> todoTasks = taskService.getTasksByStatus(TaskStatus.TODO);
            java.util.List<TaskRecord> overdue =
                    new java.util.ArrayList<>(taskService.getOverdueTasks());

            // Add TODO tasks with scheduledStartAt in the past (not started on time) -> overdue
            // indicator
            for (TaskRecord t : todoTasks) {
                if (t.scheduledStartAt() != null && t.scheduledStartAt().isBefore(now)) {
                    if (overdue.stream().noneMatch(o -> o.id().equals(t.id()))) {
                        overdue.add(t);
                    }
                }
            }
            model.addAttribute("overdueTasks", overdue);

            // Build dueToday: dueDate today OR scheduledStartAt today (even if not yet started)
            java.util.List<TaskRecord> dueToday = new java.util.ArrayList<>();
            java.util.List<TaskRecord> allStatuses = taskService.getAllTasks();
            for (TaskRecord t : allStatuses) {
                boolean dueDateToday =
                        t.dueDate() != null && t.dueDate().toLocalDate().equals(today);
                boolean scheduledToday =
                        t.scheduledStartAt() != null
                                && t.scheduledStartAt().toLocalDate().equals(today);
                if (dueDateToday || scheduledToday) {
                    dueToday.add(t);
                }
            }
            // Sort by (scheduledStartAt or dueDate) ascending for nicer table ordering
            dueToday.sort(
                    (a, b) -> {
                        java.time.LocalDateTime aKey =
                                a.scheduledStartAt() != null ? a.scheduledStartAt() : a.dueDate();
                        java.time.LocalDateTime bKey =
                                b.scheduledStartAt() != null ? b.scheduledStartAt() : b.dueDate();
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

            // Active tasks (IN_PROGRESS + IN_PAUSE + ready-to-start scheduled TODO tasks whose
            // scheduledStartAt <= now)
            java.util.List<TaskRecord> active = new java.util.ArrayList<>();
            active.addAll(taskService.getTasksByStatus(TaskStatus.IN_PROGRESS));
            active.addAll(taskService.getTasksByStatus(TaskStatus.IN_PAUSE));
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

            // Scheduled future tasks (scheduled start after now but still today or later)
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

    @GetMapping("/tasks")
    public String listTasks(
            @ModelAttribute("filter") TaskFilterForm filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<@NotNull TaskRecord> tasksPage = taskService.searchTasks(filter, pageable);
        model.addAttribute("tasksPage", tasksPage);
        model.addAttribute("filter", filter);
        return "tasks/list";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        TaskForm form = new TaskForm();
        form.setStatus(TaskStatus.TODO);
        form.setPriority(Priority.MEDIUM);
        model.addAttribute("taskForm", form);
        return "tasks/create";
    }

    @PostMapping("/tasks")
    public String createTask(
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "tasks/create";
        }
        TaskCreateRecord taskCreateRecord =
                new TaskCreateRecord(
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
        taskService.createTask(taskCreateRecord);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tarefa criada com sucesso!");
        return REDIRECT_TASKS;
    }

    @GetMapping("/tasks/{id}")
    public String editTaskForm(@PathVariable Long id, Model model) {
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
            @PathVariable Long id,
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "tasks/edit";
        }
        TaskCreateRecord taskCreateRecord =
                new TaskCreateRecord(
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
        return REDIRECT_TASKS;
    }

    @PostMapping("/tasks/{id}/status")
    public String changeStatus(
            @PathVariable Long id,
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
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "Tarefa excluída com sucesso!");
        return REDIRECT_TASKS;
    }
}
