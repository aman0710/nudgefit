package com.nudgefit.model.entity;

import com.nudgefit.model.enums.MealType;
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
@Table(name = "food_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "daily_log_id", nullable = false)
    private UUID dailyLogId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "raw_user_input", columnDefinition = "TEXT")
    private String rawUserInput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_items", columnDefinition = "jsonb")
    private String parsedItems;

    @Column(name = "total_calories", precision = 7, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "protein_g", precision = 5, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", precision = 5, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", precision = 5, scale = 2)
    private BigDecimal fatG;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", length = 10)
    private MealType mealType;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }
}
