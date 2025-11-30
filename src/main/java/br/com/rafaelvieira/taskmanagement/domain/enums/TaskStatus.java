package br.com.rafaelvieira.taskmanagement.domain.enums;

/**
 * Status possíveis de uma tarefa.
 *
 * <p>Tarefa pendente, aguardando início IN_PROGRESS - Tarefa em execução IN_PAUSE - Tarefa pausada
 * (break do pomodoro) PENDING - Tempo de execução finalizado, aguardando ação do usuário (dentro do
 * prazo) DONE - Tarefa concluída CANCELLED - Tarefa cancelada OVERDUE - Tarefa atrasada (tempo
 * excedido e/ou prazo vencido)
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_PAUSE,
    PENDING,
    DONE,
    CANCELLED,
    OVERDUE
}
