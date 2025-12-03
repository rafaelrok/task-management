package br.com.rafaelvieira.taskmanagement.domain.model;

import br.com.rafaelvieira.taskmanagement.domain.enums.BadgeLevel;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade para rastrear selos mensais de usuários baseados no número de tasks de squad concluídas
 * por mês
 */
@Entity
@Table(
        name = "user_monthly_badges",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"user_id", "reference_year", "reference_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMonthlyBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_year", nullable = false)
    private Integer referenceYear;

    @Column(name = "reference_month", nullable = false)
    private Integer referenceMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_level", nullable = false)
    @Builder.Default
    private BadgeLevel level = BadgeLevel.NONE;

    @Column(name = "tasks_completed_in_squads", nullable = false)
    @Builder.Default
    private Integer tasksCompletedInSquads = 0;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();

    @Column(name = "is_current_month", nullable = false)
    @Builder.Default
    private Boolean isCurrentMonth = false;

    /** Retorna o YearMonth de referência */
    public YearMonth getReferenceMonth() {
        return YearMonth.of(referenceYear, referenceMonth);
    }

    /** Define o mês de referência a partir de um YearMonth */
    public void setReferenceMonth(YearMonth yearMonth) {
        this.referenceYear = yearMonth.getYear();
        this.referenceMonth = yearMonth.getMonthValue();
    }

    /** Atualiza o nível do badge baseado no número de tasks */
    public void recalculateLevel() {
        this.level = BadgeLevel.fromTaskCount(this.tasksCompletedInSquads);
        this.calculatedAt = LocalDateTime.now();
    }

    /** Incrementa o contador de tasks e recalcula o nível */
    public void incrementTasksCompleted() {
        this.tasksCompletedInSquads++;
        recalculateLevel();
    }

    /** Verifica se houve descida de nível em relação ao mês anterior */
    public boolean hasLevelDropped(BadgeLevel previousLevel) {
        return this.level.ordinal() < previousLevel.ordinal();
    }

    /** Retorna uma descrição formatada do badge */
    public String getFormattedDescription() {
        return String.format(
                "%s - %s/%d (%d tasks)",
                level.getDisplayName(),
                getReferenceMonth().getMonth().name(),
                referenceYear,
                tasksCompletedInSquads);
    }
}
