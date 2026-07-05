package com.nudgefit.model.dto;

import com.nudgefit.model.enums.Intent;
import com.nudgefit.model.enums.MealType;

public record IntentClassificationResponse(
        Intent intent,
        double confidence,
        MealType meal_type
) {}
