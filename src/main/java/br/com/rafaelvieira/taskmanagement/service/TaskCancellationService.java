package br.com.rafaelvieira.taskmanagement.service;

import br.com.rafaelvieira.taskmanagement.domain.enums.TaskCancelRequestStatus;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.TaskCancelRequest;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.exception.ResourceNotFoundException;
import br.com.rafaelvieira.taskmanagement.exception.UnauthorizedException;
import br.com.rafaelvieira.taskmanagement.repository.TaskCancelRequestRepository;
import br.com.rafaelvieira.taskmanagement.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskCancellationService {

    private final TaskCancelRequestRepository cancelRequestRepository;
    private final TaskRepository taskRepository;
    private final GamificationWebSocketService webSocketService;

    @Transactional
    public void requestCancellation(Long taskId, User requestedBy, String reason) {
        Task task =
                taskRepository
                        .findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (task.getSquad() == null) {
            // Personal task, just cancel
            task.setStatus(TaskStatus.CANCELLED);
            taskRepository.save(task);
            return;
        }

        if (!task.getAssignedUser().getId().equals(requestedBy.getId())) {
            throw new UnauthorizedException("You can only request cancellation for your own tasks");
        }

        // Check if already pending
        if (task.getStatus() == TaskStatus.AUTH_PENDING) {
            throw new IllegalArgumentException("Cancellation already requested");
        }

        TaskCancelRequest request =
                TaskCancelRequest.builder()
                        .task(task)
                        .requestedBy(requestedBy)
                        .leadToApprove(task.getSquad().getLead())
                        .reason(reason)
                        .previousStatus(task.getStatus())
                        .build();

        cancelRequestRepository.save(request);
        webSocketService.notifyCancellationRequest(request);

        task.setStatus(TaskStatus.AUTH_PENDING);
        taskRepository.save(task);
    }

    public List<TaskCancelRequest> getPendingRequestsForLead(User lead) {
        return cancelRequestRepository.findByLeadToApproveAndStatus(
                lead, TaskCancelRequestStatus.PENDING);
    }

    @Transactional
    public void reviewRequest(Long requestId, boolean approve, User lead) {
        TaskCancelRequest request =
                cancelRequestRepository
                        .findById(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getLeadToApprove().getId().equals(lead.getId())) {
            throw new UnauthorizedException("Not authorized to review this request");
        }

        request.setDecidedAt(LocalDateTime.now());
        Task task = request.getTask();

        if (approve) {
            request.setStatus(TaskCancelRequestStatus.APPROVED);
            task.setStatus(TaskStatus.CANCELLED);
        } else {
            request.setStatus(TaskCancelRequestStatus.REJECTED);
            task.setStatus(request.getPreviousStatus());
        }

        cancelRequestRepository.save(request);
        taskRepository.save(task);
        webSocketService.notifyCancellationDecision(request, approve);
    }

    @Transactional
    public void approveCancellation(Long requestId, User lead) {
        reviewRequest(requestId, true, lead);
    }

    @Transactional
    public void rejectCancellation(Long requestId, User lead) {
        reviewRequest(requestId, false, lead);
    }
}
