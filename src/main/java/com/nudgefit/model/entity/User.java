package com.nudgefit.model.entity;

import com.nudgefit.model.enums.ActivityLevel;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.model.enums.FatGoal;
import com.nudgefit.model.enums.Gender;
import com.nudgefit.model.enums.IntensityLevel;
import com.nudgefit.model.enums.MuscleGoal;
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
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String name;

    @Column(name = "current_weight_kg", precision = 5, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 20)
    private ActivityLevel activityLevel;

    @Column(name = "tdee_calories", precision = 7, scale = 2)
    private BigDecimal tdeeCalories;

    @Column(name = "daily_calorie_target", precision = 7, scale = 2)
    private BigDecimal dailyCalorieTarget;

    @Enumerated(EnumType.STRING)
    @Column(name = "fat_goal", length = 10)
    private FatGoal fatGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_goal", length = 10)
    private MuscleGoal muscleGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensity_level", length = 20)
    private IntensityLevel intensityLevel;

    @Column(name = "current_body_fat_pct", precision = 5, scale = 2)
    private BigDecimal currentBodyFatPct;

    @Column(name = "target_body_fat_pct", precision = 5, scale = 2)
    private BigDecimal targetBodyFatPct;

    @Column(name = "current_muscle_mass_kg", precision = 5, scale = 2)
    private BigDecimal currentMuscleMassKg;

    @Column(name = "target_muscle_mass_kg", precision = 5, scale = 2)
    private BigDecimal targetMuscleMassKg;

    @Column(name = "daily_protein_target_g", precision = 5, scale = 2)
    private BigDecimal dailyProteinTargetG;

    @Column(name = "daily_carbs_target_g", precision = 5, scale = 2)
    private BigDecimal dailyCarbsTargetG;

    @Column(name = "daily_fat_target_g", precision = 5, scale = 2)
    private BigDecimal dailyFatTargetG;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_state", length = 30)
    private ConversationState conversationState;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
