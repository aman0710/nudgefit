package com.nudgefit.model.dto;

import com.nudgefit.model.enums.WorkoutType;

import java.math.BigDecimal;

public record ParsedWorkoutResponse(
        WorkoutType workout_type,
        Integer duration_minutes,
        BigDecimal calories_burned,
        String details
) {}
