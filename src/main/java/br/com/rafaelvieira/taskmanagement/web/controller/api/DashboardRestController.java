package br.com.rafaelvieira.taskmanagement.web.controller.api;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para estatísticas do painel. Fornece endpoints para recuperar dados para
 * gráficos de painel. Atualmente, suporta a contagem de status de tarefa. Melhorias futuras podem
 * incluir estatísticas adicionais conforme necessário.
 *
 * <p>Author: Rafael Vieira Since: 25/11/2025
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final TaskService taskService;

    @GetMapping("/stats")
    public ResponseEntity<@NotNull Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Task Status Counts
        stats.put("todo", taskService.countTasksByStatus(TaskStatus.TODO));
        stats.put("inProgress", taskService.countTasksByStatus(TaskStatus.IN_PROGRESS));
        stats.put("done", taskService.countTasksByStatus(TaskStatus.DONE));
        stats.put("cancelled", taskService.countTasksByStatus(TaskStatus.CANCELLED));

        // Priority Counts
        stats.put("countLow", taskService.countTasksByPriority(Priority.LOW));
        stats.put("countMedium", taskService.countTasksByPriority(Priority.MEDIUM));
        stats.put("countHigh", taskService.countTasksByPriority(Priority.HIGH));
        stats.put("countUrgent", taskService.countTasksByPriority(Priority.URGENT));

        return ResponseEntity.ok(stats);
    }
}
