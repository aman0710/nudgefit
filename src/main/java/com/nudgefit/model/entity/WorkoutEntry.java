package com.nudgefit.model.entity;

import com.nudgefit.model.enums.WorkoutType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workout_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "daily_log_id", nullable = false)
    private UUID dailyLogId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "raw_user_input", columnDefinition = "TEXT")
    private String rawUserInput;

    @Enumerated(EnumType.STRING)
    @Column(name = "workout_type", length = 15)
    private WorkoutType workoutType;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "calories_burned", precision = 7, scale = 2)
    private BigDecimal caloriesBurned;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }
}
