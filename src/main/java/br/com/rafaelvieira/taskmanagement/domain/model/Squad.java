package br.com.rafaelvieira.taskmanagement.domain.model;

import br.com.rafaelvieira.taskmanagement.domain.enums.SquadType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "squads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Squad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "lead_id", nullable = false)
    private User lead;

    @Enumerated(EnumType.STRING)
    @Column(name = "squad_type")
    @Builder.Default
    private SquadType type = SquadType.FULLSTACK;

    @Column(name = "tech_stack")
    @Size(max = 500, message = "Tech stack must be at most 500 characters")
    private String techStack;

    @Column(name = "business_area")
    @Size(max = 200, message = "Business area must be at most 200 characters")
    private String businessArea;

    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "max_members")
    private Integer maxMembers;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    /** Verifica se o squad está ativo */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /** Desativa o squad */
    public void deactivate() {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
    }

    /** Reativa o squad */
    public void activate() {
        this.active = true;
        this.deactivatedAt = null;
    }
}
