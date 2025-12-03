package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.enums.TaskCancelRequestStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.TaskCancelRequest;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskCancelRequestRepository extends JpaRepository<TaskCancelRequest, Long> {
    List<TaskCancelRequest> findByLeadToApproveAndStatus(User lead, TaskCancelRequestStatus status);

    List<TaskCancelRequest> findByLeadToApproveAndStatusOrderByCreatedAtDesc(
            User lead, TaskCancelRequestStatus status);

    Optional<TaskCancelRequest> findByTaskAndStatus(Task task, TaskCancelRequestStatus status);
}
