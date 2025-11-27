package br.com.rafaelvieira.taskmanagement.scheduler;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.records.DashboardUpdateMessage;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.service.TaskService;
import br.com.rafaelvieira.taskmanagement.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agenda para atualizar o dashboard via WebSocket. Executa a cada 5 segundos para enviar dados
 * sincronizados de tarefas e KPIs para todos os clientes conectados.
 *
 * <p>Author: Rafael Vieira Since: 25/11/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardUpdateScheduler {

    private final TaskService taskService;
    private final WebSocketService webSocketService;

    /**
     * Atualiza o dashboard via WebSocket a cada 5 segundos. Envia dados sincronizados de tarefas e
     * KPIs para todos os clientes conectados
     */
    @Scheduled(fixedRate = 5000)
    public void sendDashboardUpdate() {
        try {
            log.info("[DASHBOARD-WS] Executing scheduled dashboard update...");

            // Coleta as métricas do dashboard
            Long totalTodo = taskService.countTasksByStatus(TaskStatus.TODO);
            Long totalInProgress = taskService.countTasksByStatus(TaskStatus.IN_PROGRESS);
            Long totalDone = taskService.countTasksByStatus(TaskStatus.DONE);
            Long totalCancelled = taskService.countTasksByStatus(TaskStatus.CANCELLED);

            Long countLow = taskService.countTasksByPriority(Priority.LOW);
            Long countMedium = taskService.countTasksByPriority(Priority.MEDIUM);
            Long countHigh = taskService.countTasksByPriority(Priority.HIGH);
            Long countUrgent = taskService.countTasksByPriority(Priority.URGENT);

            DashboardUpdateMessage message =
                    DashboardUpdateMessage.create(
                            totalTodo,
                            totalInProgress,
                            totalDone,
                            totalCancelled,
                            countLow,
                            countMedium,
                            countHigh,
                            countUrgent);

            webSocketService.sendDashboardUpdate(message);

            log.info(
                    "[DASHBOARD-WS] Update sent successfully: TODO={}, IN_PROGRESS={}, DONE={},"
                            + " CANCELLED={}",
                    totalTodo,
                    totalInProgress,
                    totalDone,
                    totalCancelled);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            log.warn(
                    "[DASHBOARD-WS] Resource not found during dashboard update, possibly no tasks"
                            + " yet.",
                    resourceNotFoundException);
        }
    }
}
