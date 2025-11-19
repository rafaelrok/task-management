package br.com.rafaelvieira.taskmanagement.service.impl;

import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import br.com.rafaelvieira.taskmanagement.service.NotificationService;
import br.com.rafaelvieira.taskmanagement.service.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    @Override
    public long countOverdueForCurrentUser() {
        var user = userService.getCurrentUser();
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .filter(
                        t ->
                                t.getAssignedUser() != null
                                        && t.getAssignedUser().getId().equals(user.getId()))
                .count();
    }

    @Override
    public long countNearDueForCurrentUser() {
        var user = userService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime window = now.plusHours(48);
        return taskRepository.findAll().stream()
                .filter(
                        t ->
                                t.getAssignedUser() != null
                                        && t.getAssignedUser().getId().equals(user.getId()))
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(
                        t ->
                                t.getDueDate() != null
                                        && !t.getDueDate().isBefore(now)
                                        && t.getDueDate().isBefore(window))
                .count();
    }
}
