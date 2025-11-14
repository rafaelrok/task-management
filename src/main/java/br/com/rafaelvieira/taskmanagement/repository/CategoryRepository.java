package br.com.rafaelvieira.taskmanagement.repository;

import br.com.rafaelvieira.taskmanagement.domain.model.Category;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<@NotNull Category, @NotNull Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);
}
