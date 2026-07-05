package com.nudgefit.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    @Column(name = "projected_goal_date")
    private LocalDate projectedGoalDate;

    @Column(name = "daily_calorie_target", precision = 7, scale = 2)
    private BigDecimal dailyCalorieTarget;

    @Column(name = "days_remaining")
    private Integer daysRemaining;

    @Column(name = "days_impact", precision = 5, scale = 2)
    private BigDecimal daysImpact;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
