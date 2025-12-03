package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.enums.SquadType;
import br.com.rafaelvieira.taskmanagement.domain.model.Squad;
import br.com.rafaelvieira.taskmanagement.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByLead(User lead);

    List<Squad> findByLeadAndActiveTrue(User lead);

    List<Squad> findByActiveTrue();

    List<Squad> findByActiveFalse();

    List<Squad> findByType(SquadType type);

    List<Squad> findByTypeAndActiveTrue(SquadType type);

    @Query(
            "SELECT s FROM Squad s WHERE s.active = true AND "
                    + "(LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(s.techStack) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + "LOWER(s.businessArea) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Squad> searchActiveSquads(@Param("search") String search);

    @Query(
            "SELECT s FROM Squad s WHERE "
                    + "(:type IS NULL OR s.type = :type) AND "
                    + "(:active IS NULL OR s.active = :active) AND "
                    + "(:search IS NULL OR :search = '' OR "
                    + " LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
                    + " LOWER(s.techStack) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Squad> findWithFilters(
            @Param("type") SquadType type,
            @Param("active") Boolean active,
            @Param("search") String search);

    @Query("SELECT s FROM Squad s LEFT JOIN FETCH s.lead WHERE s.id = :id")
    Optional<Squad> findByIdWithLead(@Param("id") Long id);

    @Query("SELECT COUNT(sm) FROM SquadMember sm WHERE sm.squad.id = :squadId")
    Long countMembersBySquadId(@Param("squadId") Long squadId);

    @Query("SELECT s FROM Squad s WHERE s.active = true ORDER BY s.createdAt DESC")
    List<Squad> findAllActiveOrderByCreatedAtDesc();

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);
}
