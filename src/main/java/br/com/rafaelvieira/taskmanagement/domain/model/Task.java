package br.com.rafaelvieira.taskmanagement.domain.model;

import br.com.rafaelvieira.taskmanagement.domain.enums.Priority;
import br.com.rafaelvieira.taskmanagement.domain.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Column(nullable = false)
    private String title;

    // Rich HTML content from editor - unlimited length (TEXT type in DB)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // New scheduling fields
    @Column(name = "scheduled_start_at")
    private LocalDateTime scheduledStartAt;

    // Pomodoro duration in minutes for this task when started (nullable -> no pomodoro)
    @Column(name = "pomodoro_minutes")
    private Integer pomodoroMinutes;

    @Column(name = "pomodoro_break_minutes")
    @Builder.Default
    private Integer pomodoroBreakMinutes = 5;

    // ===== Execution/pomodoro tracking =====
    // Tempo alvo de execução em minutos (definido pelo dev)
    @Column(name = "execution_time_minutes")
    private Integer executionTimeMinutes;

    // Marca quando o contador principal começou pela última vez (null = parado)
    @Column(name = "main_started_at")
    private LocalDateTime mainStartedAt;

    // Acumulado em segundos no contador principal
    @Column(name = "main_elapsed_seconds")
    @Builder.Default
    private Long mainElapsedSeconds = 0L;

    // Quando o pomodoro atual termina (se em execução)
    @Column(name = "pomodoro_until")
    private LocalDateTime pomodoroUntil;

    // Tempo extra em minutos (caso precise estender a estimativa)
    @Column(name = "extra_time_minutes")
    @Builder.Default
    private Integer extraTimeMinutes = 0;

    // Justificativa para extensão de tempo
    @Column(name = "extension_justification", columnDefinition = "TEXT")
    private String extensionJustification;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TaskStatus.TODO;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
    }

    @PreUpdate
    void onUpdate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && status != TaskStatus.DONE;
    }

    public boolean isCompleted() {
        return status == TaskStatus.DONE;
    }
}
