package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.model.User;
import br.com.rafaelvieira.taskmanagement.domain.model.UserMonthlyBadge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMonthlyBadgeRepository extends JpaRepository<UserMonthlyBadge, Long> {

    Optional<UserMonthlyBadge> findByUserAndReferenceYearAndReferenceMonth(
            User user, Integer referenceYear, Integer referenceMonth);

    List<UserMonthlyBadge> findByUserOrderByReferenceYearDescReferenceMonthDesc(User user);

    @Query(
            "SELECT umb FROM UserMonthlyBadge umb WHERE umb.user = :user "
                    + "ORDER BY umb.referenceYear DESC, umb.referenceMonth DESC")
    List<UserMonthlyBadge> findAllByUserOrdered(@Param("user") User user);

    @Query(
            "SELECT umb FROM UserMonthlyBadge umb WHERE umb.user = :user "
                    + "AND umb.isCurrentMonth = true")
    Optional<UserMonthlyBadge> findCurrentMonthBadge(@Param("user") User user);

    @Query(
            "SELECT umb FROM UserMonthlyBadge umb WHERE umb.user.id = :userId "
                    + "ORDER BY umb.referenceYear DESC, umb.referenceMonth DESC LIMIT 1")
    Optional<UserMonthlyBadge> findLatestBadgeByUserId(@Param("userId") Long userId);

    @Query(
            "SELECT umb FROM UserMonthlyBadge umb WHERE "
                    + "umb.referenceYear = :year AND umb.referenceMonth = :month "
                    + "ORDER BY umb.tasksCompletedInSquads DESC")
    List<UserMonthlyBadge> findMonthlyRanking(
            @Param("year") Integer year, @Param("month") Integer month);

    @Query(
            "SELECT COUNT(umb) FROM UserMonthlyBadge umb WHERE "
                    + "umb.user = :user AND umb.level = 'DIAMANTE'")
    Long countDiamondBadgesByUser(@Param("user") User user);

    @Query("SELECT umb FROM UserMonthlyBadge umb WHERE umb.isCurrentMonth = true")
    List<UserMonthlyBadge> findAllCurrentMonth();

    @Query(
            "UPDATE UserMonthlyBadge umb SET umb.isCurrentMonth = false WHERE umb.isCurrentMonth ="
                    + " true")
    void resetAllCurrentMonth();
}
