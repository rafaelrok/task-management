package br.com.rafaelvieira.taskmanagement.domain.records;

import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import java.util.List;

public record SquadDashboardDTO(
        Squad squad,
        int totalTasks,
        int inProgressTasks,
        int completedTasks,
        int todoTasks,
        int overdueTasks,
        int memberCount,
        List<Task> activeTimerTasks,
        List<MemberMetricsDTO> memberMetrics) {}
