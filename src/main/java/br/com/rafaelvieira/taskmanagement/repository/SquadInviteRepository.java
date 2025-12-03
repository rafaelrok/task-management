package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.enums.SquadInviteStatus;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadInvite;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SquadInviteRepository extends JpaRepository<SquadInvite, Long> {

    @Query(
            "SELECT si FROM SquadInvite si "
                    + "LEFT JOIN FETCH si.squad s "
                    + "LEFT JOIN FETCH s.lead "
                    + "LEFT JOIN FETCH si.invitedBy "
                    + "WHERE si.invitedUser = :user AND si.status = :status")
    List<SquadInvite> findByInvitedUserAndStatus(
            @Param("user") User invitedUser, @Param("status") SquadInviteStatus status);

    @Query(
            "SELECT si FROM SquadInvite si "
                    + "LEFT JOIN FETCH si.squad s "
                    + "LEFT JOIN FETCH s.lead "
                    + "LEFT JOIN FETCH si.invitedBy "
                    + "WHERE si.invitedUser = :user AND si.status IN :statuses "
                    + "ORDER BY si.respondedAt DESC, si.createdAt DESC")
    List<SquadInvite> findByInvitedUserAndStatusIn(
            @Param("user") User invitedUser, @Param("statuses") List<SquadInviteStatus> statuses);

    List<SquadInvite> findBySquadAndStatus(Squad squad, SquadInviteStatus status);

    @Query(
            "SELECT si FROM SquadInvite si "
                    + "LEFT JOIN FETCH si.squad s "
                    + "LEFT JOIN FETCH s.lead "
                    + "LEFT JOIN FETCH si.invitedUser "
                    + "LEFT JOIN FETCH si.invitedBy "
                    + "WHERE si.id = :id")
    Optional<SquadInvite> findByIdWithDetails(@Param("id") Long id);
}
