package br.com.rafaelvieira.taskmanagement.service;

public interface NotificationService {
    long countOverdueForCurrentUser();

    long countNearDueForCurrentUser();
}
