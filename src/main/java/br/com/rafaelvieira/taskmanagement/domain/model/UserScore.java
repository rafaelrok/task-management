package br.com.rafaelvieira.taskmanagement.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points")
    @Builder.Default
    private int totalPoints = 0;

    @Column(name = "total_tasks_completed_in_squads")
    @Builder.Default
    private int totalTasksCompletedInSquads = 0;

    @Column(name = "total_tasks_completed_early")
    @Builder.Default
    private int totalTasksCompletedEarly = 0;

    @Column(name = "current_level")
    @Builder.Default
    private int level = 1;
}
