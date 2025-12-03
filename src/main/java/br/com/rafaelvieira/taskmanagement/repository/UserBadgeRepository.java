package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.model.Badge;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserBadge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUser(User user);

    boolean existsByUserAndBadge(User user, Badge badge);
}
