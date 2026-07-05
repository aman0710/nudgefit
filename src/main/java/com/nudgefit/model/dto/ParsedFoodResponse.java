package com.nudgefit.model.dto;

import com.nudgefit.model.enums.MealType;

import java.math.BigDecimal;
import java.util.List;

public record ParsedFoodResponse(
        List<FoodItem> items,
        BigDecimal total_calories,
        BigDecimal total_protein_g,
        BigDecimal total_carbs_g,
        BigDecimal total_fat_g,
        MealType meal_type
) {
    public record FoodItem(
            String name,
            String quantity,
            String unit,
            BigDecimal calories,
            BigDecimal protein_g,
            BigDecimal carbs_g,
            BigDecimal fat_g
    ) {}
}
