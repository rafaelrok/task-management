package br.com.rafaelvieira.taskmanagement.domain.enums;

/** Tipos de notificação do sistema. */
public enum NotificationType {
    TASK_STARTED,
    TASK_PAUSED,
    TASK_RESUMED,
    TASK_FINISHED,
    TASK_CREATED,
    TASK_UPDATED,
    TASK_OVERDUE,
    TASK_TIME_UP,
    TASK_PENDING, // Tempo finalizado, aguardando ação
    TASK_STARTING_SOON, // Tarefa agendada prestes a iniciar (5 min)
    TASK_DUE_SOON, // Tarefa prestes a vencer
    TASK_TODO, // Tarefa em estado TODO
    SQUAD_INVITE // Convite para participar de uma squad
}
