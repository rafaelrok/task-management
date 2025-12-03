package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.SquadMember;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SquadMemberRepository extends JpaRepository<SquadMember, Long> {
    List<SquadMember> findByUser(User user);

    List<SquadMember> findBySquad(Squad squad);

    Optional<SquadMember> findBySquadAndUser(Squad squad, User user);

    boolean existsBySquadAndUser(Squad squad, User user);

    @org.springframework.data.jpa.repository.Query(
            "SELECT sm FROM SquadMember sm JOIN FETCH sm.user WHERE sm.squad.id = :squadId")
    List<SquadMember> findBySquadId(
            @org.springframework.data.repository.query.Param("squadId") Long squadId);
}
