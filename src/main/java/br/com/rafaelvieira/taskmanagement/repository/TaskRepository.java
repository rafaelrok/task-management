package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.Task;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<@NotNull Task, @NotNull Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByCategoryId(Long categoryId);

    List<Task> findByAssignedUserId(Long userId);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :date AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("date") LocalDateTime date);

    @Query("SELECT t FROM Task t WHERE t.assignedUser.id = :userId AND t.status = :status")
    List<Task> findByUserIdAndStatus(
            @Param("userId") Long userId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(@Param("status") TaskStatus status);

    List<Task> findByTitleContainingIgnoreCase(String title);

    // ========== Squad-related queries ==========

    List<Task> findBySquad(Squad squad);

    List<Task> findBySquadId(Long squadId);

    @Query("SELECT t FROM Task t WHERE t.squad.id = :squadId AND t.status = :status")
    List<Task> findBySquadIdAndStatus(
            @Param("squadId") Long squadId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.squad.id = :squadId")
    Long countBySquadId(@Param("squadId") Long squadId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.squad.id = :squadId AND t.status = :status")
    Long countBySquadIdAndStatus(
            @Param("squadId") Long squadId, @Param("status") TaskStatus status);

    @Query(
            "SELECT COUNT(t) FROM Task t WHERE t.squad.id = :squadId AND t.dueDate < :now AND"
                    + " t.status NOT IN ('DONE', 'CANCELLED')")
    Long countOverdueBySquadId(@Param("squadId") Long squadId, @Param("now") LocalDateTime now);

    // ========== User visibility queries ==========

    @Query("SELECT t FROM Task t WHERE t.assignedUser = :user AND t.squad IS NULL")
    List<Task> findPersonalTasksByUser(@Param("user") User user);

    @Query(
            "SELECT t FROM Task t WHERE t.squad.id IN "
                    + "(SELECT sm.squad.id FROM SquadMember sm WHERE sm.user = :user)")
    List<Task> findSquadTasksForMember(@Param("user") User user);

    // MEMBER: apenas tasks onde é responsável (assignedUser)
    @Query("SELECT DISTINCT t FROM Task t WHERE t.assignedUser = :user")
    List<Task> findVisibleTasksForMember(@Param("user") User user);

    @Query("SELECT t FROM Task t WHERE t.squad.lead = :lead")
    List<Task> findTasksForLead(@Param("lead") User lead);

    // LEAD/ADMIN: todas as tasks de squads que lidera (qualquer responsável)
    @Query("SELECT DISTINCT t FROM Task t WHERE t.squad IS NOT NULL AND t.squad.lead = :user")
    List<Task> findVisibleTasksForLead(@Param("user") User user);

    // ========== Gamification queries ==========
    @Query(
            "SELECT COUNT(t) FROM Task t WHERE "
                    + "t.assignedUser = :user AND "
                    + "t.squad IS NOT NULL AND "
                    + "t.status = 'DONE' AND "
                    + "YEAR(t.completedAt) = :year AND MONTH(t.completedAt) = :month")
    Long countSquadTasksCompletedInMonth(
            @Param("user") User user, @Param("year") int year, @Param("month") int month);

    @Query(
            "SELECT t FROM Task t LEFT JOIN FETCH t.assignedUser WHERE t.squad.id = :squadId AND"
                    + " t.status = :status")
    List<Task> findActiveTimerTasksBySquadId(
            @Param("squadId") Long squadId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.squad.id = :squadId AND t.assignedUser.id = :userId")
    List<Task> findBySquadIdAndAssignedUserId(
            @Param("squadId") Long squadId, @Param("userId") Long userId);

    @Query(
            "SELECT COUNT(t) FROM Task t WHERE "
                    + "t.squad.id = :squadId AND t.assignedUser.id = :userId AND t.status = 'DONE'")
    Long countCompletedBySquadIdAndUserId(
            @Param("squadId") Long squadId, @Param("userId") Long userId);

    // ========== Role-based filtered queries ==========
    // LEAD/ADMIN: todas as tasks de squads que lidera (qualquer responsável)
    @Query(
            "SELECT COUNT(t) FROM Task t WHERE t.status = :status AND t.squad IS NOT NULL AND"
                    + " t.squad.lead = :user")
    long countByStatusForLead(@Param("status") TaskStatus status, @Param("user") User user);

    // MEMBER: apenas tasks onde é responsável (assignedUser)
    @Query(
            "SELECT COUNT(DISTINCT t) FROM Task t WHERE t.status = :status AND t.assignedUser ="
                    + " :user")
    long countByStatusForMember(@Param("status") TaskStatus status, @Param("user") User user);

    // LEAD/ADMIN: todas as tasks de squads que lidera (qualquer responsável)
    @Query(
            "SELECT t FROM Task t WHERE t.status = :status AND t.squad IS NOT NULL AND t.squad.lead"
                    + " = :user")
    List<Task> findByStatusForLead(@Param("status") TaskStatus status, @Param("user") User user);

    // MEMBER: apenas tasks onde é responsável (assignedUser)
    @Query("SELECT DISTINCT t FROM Task t WHERE t.status = :status AND t.assignedUser = :user")
    List<Task> findByStatusForMember(@Param("status") TaskStatus status, @Param("user") User user);

    // LEAD/ADMIN: todas as tasks de squads que lidera (qualquer responsável)
    @Query(
            "SELECT t FROM Task t WHERE t.dueDate < :date AND t.status != 'DONE' AND t.squad IS NOT"
                    + " NULL AND t.squad.lead = :user")
    List<Task> findOverdueTasksForLead(@Param("date") LocalDateTime date, @Param("user") User user);

    // MEMBER: apenas tasks onde é responsável (assignedUser)
    @Query(
            "SELECT DISTINCT t FROM Task t WHERE t.dueDate < :date AND t.status != 'DONE' AND"
                    + " t.assignedUser = :user")
    List<Task> findOverdueTasksForMember(
            @Param("date") LocalDateTime date, @Param("user") User user);

    // LEAD/ADMIN: todas as tasks de squads que lidera (qualquer responsável)
    @Query(
            "SELECT COUNT(t) FROM Task t WHERE t.priority = :priority AND t.squad IS NOT NULL AND"
                    + " t.squad.lead = :user")
    long countByPriorityForLead(@Param("priority") Priority priority, @Param("user") User user);

    // MEMBER: apenas tasks onde é responsável (assignedUser)
    @Query(
            "SELECT COUNT(DISTINCT t) FROM Task t WHERE t.priority = :priority AND t.assignedUser ="
                    + " :user")
    long countByPriorityForMember(@Param("priority") Priority priority, @Param("user") User user);
}
