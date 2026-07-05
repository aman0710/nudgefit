package com.nudgefit.model.entity;

import com.nudgefit.model.enums.Compliance;
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
@Table(name = "daily_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "log_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "total_calories_consumed", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal totalCaloriesConsumed = BigDecimal.ZERO;

    @Column(name = "total_calories_burned", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal totalCaloriesBurned = BigDecimal.ZERO;

    @Column(name = "net_calories", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal netCalories = BigDecimal.ZERO;

    @Column(name = "target_calories", precision = 7, scale = 2)
    private BigDecimal targetCalories;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Compliance compliance;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
