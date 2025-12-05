package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.entity.Notification;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    /** Encontra notificações sticky não lidas para um usuário. */
    List<Notification> findByUserAndReadFalseAndStickyTrueOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    boolean existsByUserAndTaskIdAndTypeAndReadFalse(
            User user,
            Long taskId,
            br.com.rafaelvieira.taskmanagement.domain.enums.NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);
}
