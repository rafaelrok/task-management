package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.model.PomodoroSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {

    @Query("SELECT p FROM PomodoroSession p WHERE p.task.id = :taskId ORDER BY p.startedAt DESC")
    List<PomodoroSession> findByTask(@Param("taskId") Long taskId);

    @Query("SELECT p FROM PomodoroSession p WHERE p.task.id = :taskId AND p.endedAt IS NULL")
    List<PomodoroSession> findActiveByTask(@Param("taskId") Long taskId);
}
