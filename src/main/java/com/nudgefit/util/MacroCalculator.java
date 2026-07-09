package com.nudgefit.util;

import com.nudgefit.model.enums.ActivityLevel;
import com.nudgefit.model.enums.FatGoal;
import com.nudgefit.model.enums.Gender;
import com.nudgefit.model.enums.IntensityLevel;
import com.nudgefit.model.enums.MuscleGoal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for macro and calorie calculations based on Body Composition goals.
 * Uses the Mifflin-St Jeor equation for BMR.
 */
public final class MacroCalculator {

    private static final BigDecimal MIN_DAILY_CALORIES = new BigDecimal("1200");
    private static final BigDecimal PROTEIN_PER_KG_LBM = new BigDecimal("2.2");
    private static final BigDecimal FAT_PER_KG_TOTAL = new BigDecimal("0.8");

    private MacroCalculator() {
        // Utility class
    }

    /**
     * BMR using Mifflin-St Jeor Equation.
     */
    public static BigDecimal calculateBmr(BigDecimal weightKg, BigDecimal heightCm, int age, Gender gender) {
        BigDecimal bmr = weightKg.multiply(new BigDecimal("10"))
                .add(heightCm.multiply(new BigDecimal("6.25")))
                .subtract(new BigDecimal(age).multiply(new BigDecimal("5")));

        if (gender == Gender.MALE) {
            bmr = bmr.add(new BigDecimal("5"));
        } else {
            bmr = bmr.subtract(new BigDecimal("161"));
        }

        return bmr.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * TDEE = BMR × Activity Multiplier.
     */
    public static BigDecimal calculateTdee(BigDecimal bmr, ActivityLevel activityLevel) {
        BigDecimal multiplier = getActivityMultiplier(activityLevel);
        return bmr.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Daily calorie target based on Intensity Level and Body Comp goals.
     */
    public static BigDecimal calculateDailyCalorieTarget(
            BigDecimal tdee,
            FatGoal fatGoal,
            MuscleGoal muscleGoal,
            IntensityLevel intensityLevel) {

        BigDecimal target = tdee;

        if (fatGoal == FatGoal.LOSE && muscleGoal == MuscleGoal.GAIN) {
            // Recomp: slight deficit to maintenance
            target = switch (intensityLevel) {
                case GRADUAL -> tdee.subtract(new BigDecimal("100"));
                case BALANCED -> tdee.subtract(new BigDecimal("200"));
                case AGGRESSIVE -> tdee.subtract(new BigDecimal("300"));
            };
        } else if (fatGoal == FatGoal.LOSE) {
            // Pure fat loss
            target = switch (intensityLevel) {
                case GRADUAL -> tdee.subtract(new BigDecimal("250"));
                case BALANCED -> tdee.subtract(new BigDecimal("500"));
                case AGGRESSIVE -> tdee.subtract(new BigDecimal("750"));
            };
        } else if (muscleGoal == MuscleGoal.GAIN && fatGoal != FatGoal.LOSE) {
            // Lean bulk / bulk
            target = switch (intensityLevel) {
                case GRADUAL -> tdee.add(new BigDecimal("150"));
                case BALANCED -> tdee.add(new BigDecimal("300"));
                case AGGRESSIVE -> tdee.add(new BigDecimal("500"));
            };
        } else if (fatGoal == FatGoal.GAIN) {
            // Just gaining weight/fat
            target = switch (intensityLevel) {
                case GRADUAL -> tdee.add(new BigDecimal("200"));
                case BALANCED -> tdee.add(new BigDecimal("400"));
                case AGGRESSIVE -> tdee.add(new BigDecimal("600"));
            };
        }

        // Safety cap
        if (target.compareTo(MIN_DAILY_CALORIES) < 0) {
            target = MIN_DAILY_CALORIES;
        }

        return target.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the daily protein target in grams.
     * Uses 2.2g per kg of Lean Body Mass (LBM).
     */
    public static BigDecimal calculateProteinTarget(BigDecimal currentWeightKg, BigDecimal currentBodyFatPct) {
        // LBM = Weight * (1 - (BF% / 100))
        BigDecimal bfDecimal = currentBodyFatPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal leanMassRatio = BigDecimal.ONE.subtract(bfDecimal);
        BigDecimal leanBodyMass = currentWeightKg.multiply(leanMassRatio);

        return leanBodyMass.multiply(PROTEIN_PER_KG_LBM).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the daily fat target in grams.
     * Uses 0.8g per kg of total body weight.
     */
    public static BigDecimal calculateFatTarget(BigDecimal currentWeightKg) {
        return currentWeightKg.multiply(FAT_PER_KG_TOTAL).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the daily carb target in grams.
     * Carbs = (Total Calories - (Protein * 4) - (Fat * 9)) / 4
     */
    public static BigDecimal calculateCarbsTarget(BigDecimal dailyCalorieTarget, BigDecimal proteinG, BigDecimal fatG) {
        BigDecimal proteinCalories = proteinG.multiply(new BigDecimal("4"));
        BigDecimal fatCalories = fatG.multiply(new BigDecimal("9"));
        BigDecimal allocatedCalories = proteinCalories.add(fatCalories);

        BigDecimal remainingCalories = dailyCalorieTarget.subtract(allocatedCalories);

        // If for some reason remaining is negative, cap at 0
        if (remainingCalories.compareTo(BigDecimal.ZERO) < 0) {
            remainingCalories = BigDecimal.ZERO;
        }

        return remainingCalories.divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Estimates the timeline in days based on body composition goals and intensity.
     * Note: This is a rough estimation for coaching purposes.
     * 1 kg of fat loss requires ~7700 kcal deficit.
     * 1 kg of muscle gain requires ~7000 kcal surplus.
     */
    public static int calculateEstimatedTimelineDays(
            BigDecimal currentWeightKg,
            BigDecimal currentBodyFatPct,
            BigDecimal targetBodyFatPct,
            BigDecimal currentMuscleMassKg,
            BigDecimal targetMuscleMassKg,
            FatGoal fatGoal,
            MuscleGoal muscleGoal,
            IntensityLevel intensityLevel) {

        int daysForFat = 0;
        int daysForMuscle = 0;

        // Fat Loss calculation (based on realistic weekly body fat % drop)
        if (fatGoal == FatGoal.LOSE && currentBodyFatPct != null && targetBodyFatPct != null && currentBodyFatPct.compareTo(targetBodyFatPct) > 0) {
            BigDecimal pctDiff = currentBodyFatPct.subtract(targetBodyFatPct);
            
            // Realistic weekly BF% drop (Conservative estimates)
            BigDecimal weeklyDropPct = switch (intensityLevel) {
                case GRADUAL -> new BigDecimal("0.10"); // Extremely slow, ~1 year for 5%
                case BALANCED -> new BigDecimal("0.20"); // Moderate, ~6 months for 5%
                case AGGRESSIVE -> new BigDecimal("0.35"); // Strict, ~3.5 months for 5%
            };
            
            BigDecimal weeksRequired = pctDiff.divide(weeklyDropPct, 1, RoundingMode.HALF_UP);
            daysForFat = weeksRequired.multiply(new BigDecimal("7")).intValue();
        }

        // Muscle Gain calculation (based on realistic weekly kg gain for naturals)
        if (muscleGoal == MuscleGoal.GAIN && currentMuscleMassKg != null && targetMuscleMassKg != null && targetMuscleMassKg.compareTo(currentMuscleMassKg) > 0) {
            BigDecimal muscleToGainKg = targetMuscleMassKg.subtract(currentMuscleMassKg);
            
            // Realistic weekly muscle gain in kg (Highly conservative)
            BigDecimal weeklyGainKg = switch (intensityLevel) {
                case GRADUAL -> new BigDecimal("0.03"); // ~0.12kg per month (Advanced lifters)
                case BALANCED -> new BigDecimal("0.06"); // ~0.25kg per month (Intermediates)
                case AGGRESSIVE -> new BigDecimal("0.12"); // ~0.5kg per month (Beginner "newbie gains")
            };
            
            BigDecimal weeksRequired = muscleToGainKg.divide(weeklyGainKg, 1, RoundingMode.HALF_UP);
            daysForMuscle = weeksRequired.multiply(new BigDecimal("7")).intValue();
        }

        return Math.max(daysForFat, daysForMuscle);
    }

    /**
     * Converts raw days into a rough, human-readable estimate in months.
     */
    public static String formatEstimatedTimeline(int days) {
        if (days < 30) {
            return "less than a month";
        }
        int months = (int) Math.round(days / 30.0);
        if (months == 1) {
            return "about 1 month";
        }
        return "about " + months + " months";
    }

    private static BigDecimal getActivityMultiplier(ActivityLevel level) {
        return switch (level) {
            case SEDENTARY -> new BigDecimal("1.2");
            case MODERATE -> new BigDecimal("1.375");
            case ACTIVE -> new BigDecimal("1.725");
        };
    }

    /**
     * Calculates recommended workout frequency based on Intensity, Muscle Goal, and Fat Goal.
     */
    public static String calculateWorkoutRecommendations(IntensityLevel intensityLevel, MuscleGoal muscleGoal, FatGoal fatGoal) {
        int strengthDays = 0;
        int cardioDays = 0;

        if (fatGoal == FatGoal.LOSE && muscleGoal == MuscleGoal.GAIN) {
            // Recomp: high strength, moderate cardio
            strengthDays = switch (intensityLevel) { case GRADUAL -> 3; case BALANCED -> 4; case AGGRESSIVE -> 5; };
            cardioDays = switch (intensityLevel) { case GRADUAL -> 2; case BALANCED -> 3; case AGGRESSIVE -> 3; };
        } else if (fatGoal == FatGoal.LOSE) {
            // Pure fat loss: moderate strength, high cardio
            strengthDays = switch (intensityLevel) { case GRADUAL -> 2; case BALANCED -> 3; case AGGRESSIVE -> 3; };
            cardioDays = switch (intensityLevel) { case GRADUAL -> 3; case BALANCED -> 4; case AGGRESSIVE -> 5; };
        } else if (muscleGoal == MuscleGoal.GAIN && fatGoal != FatGoal.LOSE) {
            // Lean bulk / bulk: high strength, no cardio to preserve calories
            strengthDays = switch (intensityLevel) { case GRADUAL -> 3; case BALANCED -> 4; case AGGRESSIVE -> 5; };
            cardioDays = 0;
        } else if (fatGoal == FatGoal.GAIN) {
            // Just gaining weight/fat: low strength, no cardio
            strengthDays = switch (intensityLevel) { case GRADUAL -> 2; case BALANCED -> 2; case AGGRESSIVE -> 3; };
            cardioDays = 0;
        } else {
            // MAINTAIN both: balanced strength, no cardio
            strengthDays = switch (intensityLevel) { case GRADUAL -> 2; case BALANCED -> 3; case AGGRESSIVE -> 4; };
            cardioDays = 0;
        }
        
        return strengthDays + " days of strength training and " + cardioDays + " days of cardio per week";
    }
}
