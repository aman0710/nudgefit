package com.nudgefit.service;

import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.*;
import com.nudgefit.repository.UserRepository;
import com.nudgefit.util.MacroCalculator;
import com.nudgefit.ai.GeminiAiService;
import com.nudgefit.ai.PromptBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final ConversationContextService contextService;
    private final GeminiAiService geminiAiService;
    private final PromptBuilder promptBuilder;

    @Transactional
    public String handleOnboarding(User user, String message) {
        ConversationState currentState = user.getConversationState();
        if (currentState == null || currentState == ConversationState.NEW_USER) {
            return handleNewUser(user);
        }

        try {
            String response = switch (currentState) {
                case ONBOARDING_NAME -> handleName(user, message);
                case ONBOARDING_CURRENT_WEIGHT -> handleCurrentWeight(user, message);
                case ONBOARDING_HEIGHT -> handleHeight(user, message);
                case ONBOARDING_AGE -> handleAge(user, message);
                case ONBOARDING_GENDER -> handleGender(user, message);
                case ONBOARDING_ACTIVITY_LEVEL -> handleActivityLevel(user, message);
                case ONBOARDING_FAT_GOAL -> handleFatGoal(user, message);
                case ONBOARDING_CURRENT_BODY_FAT -> handleCurrentBodyFat(user, message);
                case ONBOARDING_TARGET_BODY_FAT -> handleTargetBodyFat(user, message);
                case ONBOARDING_MUSCLE_GOAL -> handleMuscleGoal(user, message);
                case ONBOARDING_CURRENT_MUSCLE_MASS -> handleCurrentMuscleMass(user, message);
                case ONBOARDING_TARGET_MUSCLE_MASS -> handleTargetMuscleMass(user, message);
                case ONBOARDING_INTENSITY_LEVEL -> handleIntensityLevel(user, message);
                case ACTIVE -> "You are already fully onboarded!";
                default -> "I got confused. Let's start over. What's your name?";
            };
            userRepository.save(user);
            contextService.setState(user.getPhoneNumber(), user.getConversationState());
            return response;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    private String handleNewUser(User user) {
        user.setConversationState(ConversationState.ONBOARDING_NAME);
        userRepository.save(user);
        contextService.setState(user.getPhoneNumber(), ConversationState.ONBOARDING_NAME);
        return "Hey! I'm NudgeFit, your AI fitness coach on WhatsApp! 💪 What's your name?";
    }

    private String handleName(User user, String message) {
        user.setName(message.trim());
        user.setConversationState(ConversationState.ONBOARDING_CURRENT_WEIGHT);
        return "Nice to meet you " + user.getName() + "! What's your current weight in kg?";
    }

    private String handleCurrentWeight(User user, String message) {
        try {
            BigDecimal weight = new BigDecimal(message.trim());
            user.setCurrentWeightKg(weight);
            user.setConversationState(ConversationState.ONBOARDING_HEIGHT);
            return "Got it. What's your height in cm?";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number for your weight in kg (e.g., 75).");
        }
    }

    private String handleHeight(User user, String message) {
        try {
            BigDecimal height = new BigDecimal(message.trim());
            user.setHeightCm(height);
            user.setConversationState(ConversationState.ONBOARDING_AGE);
            return "How old are you?";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number for your height in cm (e.g., 175).");
        }
    }

    private String handleAge(User user, String message) {
        try {
            int age = Integer.parseInt(message.trim());
            user.setAge(age);
            user.setConversationState(ConversationState.ONBOARDING_GENDER);
            return "Gender? (Male/Female)";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid age (e.g., 27).");
        }
    }

    private String handleGender(User user, String message) {
        String input = message.trim().toUpperCase();
        if (input.startsWith("M")) {
            user.setGender(Gender.MALE);
        } else if (input.startsWith("F")) {
            user.setGender(Gender.FEMALE);
        } else {
            throw new IllegalArgumentException("Please reply with Male or Female.");
        }
        user.setConversationState(ConversationState.ONBOARDING_ACTIVITY_LEVEL);
        return "How active are you?\n1. Sedentary (desk job)\n2. Lightly active (1-2 days/week)\n3. Moderately active (3-5 days/week)\n4. Very active (6-7 days/week)\nReply with 1, 2, 3, or 4.";
    }

    private String handleActivityLevel(User user, String message) {
        String input = message.trim();
        if (input.contains("1") || input.toLowerCase().contains("sedentary")) {
            user.setActivityLevel(ActivityLevel.SEDENTARY);
        } else if (input.contains("2") || input.toLowerCase().contains("lightly")) {
            user.setActivityLevel(ActivityLevel.LIGHTLY_ACTIVE);
        } else if (input.contains("3") || input.toLowerCase().contains("moderately")) {
            user.setActivityLevel(ActivityLevel.MODERATELY_ACTIVE);
        } else if (input.contains("4") || input.toLowerCase().contains("very")) {
            user.setActivityLevel(ActivityLevel.VERY_ACTIVE);
        } else {
            throw new IllegalArgumentException("Please reply with 1, 2, 3, or 4 for your activity level.");
        }
        
        user.setConversationState(ConversationState.ONBOARDING_FAT_GOAL);
        return "Awesome! Let's talk body fat goals. Do you want to LOSE, MAINTAIN, or GAIN body fat?";
    }

    private String handleFatGoal(User user, String message) {
        String input = message.trim().toUpperCase();
        if (input.contains("LOSE")) user.setFatGoal(FatGoal.LOSE);
        else if (input.contains("MAINTAIN")) user.setFatGoal(FatGoal.MAINTAIN);
        else if (input.contains("GAIN")) user.setFatGoal(FatGoal.GAIN);
        else throw new IllegalArgumentException("Please reply with LOSE, MAINTAIN, or GAIN.");

        user.setConversationState(ConversationState.ONBOARDING_CURRENT_BODY_FAT);
        return "Got it. Roughly, what is your current body fat percentage? (If you don't know, take a guess, e.g., 20).";
    }

    private String handleCurrentBodyFat(User user, String message) {
        try {
            BigDecimal bodyFat = new BigDecimal(message.replaceAll("[^0-9.]", ""));
            user.setCurrentBodyFatPct(bodyFat);
            
            if (user.getFatGoal() == FatGoal.MAINTAIN) {
                user.setTargetBodyFatPct(bodyFat);
                user.setConversationState(ConversationState.ONBOARDING_MUSCLE_GOAL);
                return "Since you want to maintain, we will keep it at " + bodyFat + "%. Now, what about muscle? Do you want to LOSE, MAINTAIN, or GAIN muscle mass?";
            } else {
                user.setConversationState(ConversationState.ONBOARDING_TARGET_BODY_FAT);
                return "And what is your target body fat percentage?";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Please enter a valid number for body fat percentage.");
        }
    }

    private String handleTargetBodyFat(User user, String message) {
        try {
            BigDecimal targetBodyFat = new BigDecimal(message.replaceAll("[^0-9.]", ""));
            
            if (user.getFatGoal() == FatGoal.LOSE && targetBodyFat.compareTo(user.getCurrentBodyFatPct()) >= 0) {
                throw new IllegalArgumentException("You said you want to LOSE fat, but your target is higher or equal to your current! Please enter a lower number.");
            }
            if (user.getFatGoal() == FatGoal.GAIN && targetBodyFat.compareTo(user.getCurrentBodyFatPct()) <= 0) {
                throw new IllegalArgumentException("You said you want to GAIN fat, but your target is lower or equal to your current! Please enter a higher number.");
            }
            
            user.setTargetBodyFatPct(targetBodyFat);
            user.setConversationState(ConversationState.ONBOARDING_MUSCLE_GOAL);
            return "Great. Now what about muscle? Do you want to LOSE, MAINTAIN, or GAIN muscle mass?";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number for target body fat percentage.");
        }
    }

    private String handleMuscleGoal(User user, String message) {
        String input = message.trim().toUpperCase();
        if (input.contains("LOSE")) user.setMuscleGoal(MuscleGoal.LOSE);
        else if (input.contains("MAINTAIN")) user.setMuscleGoal(MuscleGoal.MAINTAIN);
        else if (input.contains("GAIN")) user.setMuscleGoal(MuscleGoal.GAIN);
        else throw new IllegalArgumentException("Please reply with LOSE, MAINTAIN, or GAIN.");

        user.setConversationState(ConversationState.ONBOARDING_CURRENT_MUSCLE_MASS);
        return "Roughly, what is your current skeletal muscle mass in kg? (Guess if you aren't sure, e.g., 35).";
    }

    private String handleCurrentMuscleMass(User user, String message) {
        try {
            BigDecimal muscleMass = new BigDecimal(message.replaceAll("[^0-9.]", ""));
            user.setCurrentMuscleMassKg(muscleMass);
            
            if (user.getMuscleGoal() == MuscleGoal.MAINTAIN) {
                user.setTargetMuscleMassKg(muscleMass);
                user.setConversationState(ConversationState.ONBOARDING_INTENSITY_LEVEL);
                return "Since you want to maintain, we will target " + muscleMass + "kg. Finally, what intensity of change do you want? (GRADUAL, BALANCED, or AGGRESSIVE)";
            } else {
                user.setConversationState(ConversationState.ONBOARDING_TARGET_MUSCLE_MASS);
                return "And what is your target muscle mass in kg?";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Please enter a valid number for current muscle mass in kg.");
        }
    }

    private String handleTargetMuscleMass(User user, String message) {
        try {
            BigDecimal targetMuscleMass = new BigDecimal(message.replaceAll("[^0-9.]", ""));
            
            if (user.getMuscleGoal() == MuscleGoal.LOSE && targetMuscleMass.compareTo(user.getCurrentMuscleMassKg()) >= 0) {
                throw new IllegalArgumentException("You said you want to LOSE muscle, but your target is higher or equal to your current! Please enter a lower number.");
            }
            if (user.getMuscleGoal() == MuscleGoal.GAIN && targetMuscleMass.compareTo(user.getCurrentMuscleMassKg()) <= 0) {
                throw new IllegalArgumentException("You said you want to GAIN muscle, but your target is lower or equal to your current! Please enter a higher number.");
            }
            
            user.setTargetMuscleMassKg(targetMuscleMass);
            user.setConversationState(ConversationState.ONBOARDING_INTENSITY_LEVEL);
            return "Finally, what intensity of change do you want? (GRADUAL, BALANCED, or AGGRESSIVE)";
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number for target muscle mass in kg.");
        }
    }

    private String handleIntensityLevel(User user, String message) {
        String input = message.trim().toUpperCase();
        if (input.contains("GRADUAL")) user.setIntensityLevel(IntensityLevel.GRADUAL);
        else if (input.contains("BALANCED")) user.setIntensityLevel(IntensityLevel.BALANCED);
        else if (input.contains("AGGRESSIVE")) user.setIntensityLevel(IntensityLevel.AGGRESSIVE);
        else throw new IllegalArgumentException("Please reply with GRADUAL, BALANCED, or AGGRESSIVE.");

        // Finish onboarding: calculate macros!
        BigDecimal bmr = MacroCalculator.calculateBmr(user.getCurrentWeightKg(), user.getHeightCm(), user.getAge(), user.getGender());
        BigDecimal tdee = MacroCalculator.calculateTdee(bmr, user.getActivityLevel());
        BigDecimal dailyCalorieTarget = MacroCalculator.calculateDailyCalorieTarget(tdee, user.getFatGoal(), user.getMuscleGoal(), user.getIntensityLevel());
        
        BigDecimal proteinTarget = MacroCalculator.calculateProteinTarget(user.getCurrentWeightKg(), user.getCurrentBodyFatPct());
        BigDecimal fatTarget = MacroCalculator.calculateFatTarget(user.getCurrentWeightKg());
        BigDecimal carbsTarget = MacroCalculator.calculateCarbsTarget(dailyCalorieTarget, proteinTarget, fatTarget);

        user.setTdeeCalories(tdee);
        user.setDailyCalorieTarget(dailyCalorieTarget);
        user.setDailyProteinTargetG(proteinTarget);
        user.setDailyFatTargetG(fatTarget);
        user.setDailyCarbsTargetG(carbsTarget);

        user.setConversationState(ConversationState.ACTIVE);

        int estimatedDays = MacroCalculator.calculateEstimatedTimelineDays(
            user.getCurrentWeightKg(), user.getCurrentBodyFatPct(), user.getTargetBodyFatPct(),
            user.getCurrentMuscleMassKg(), user.getTargetMuscleMassKg(),
            user.getFatGoal(), user.getMuscleGoal(), user.getIntensityLevel()
        );
        
        String workoutRec = MacroCalculator.calculateWorkoutRecommendations(user.getIntensityLevel(), user.getMuscleGoal(), user.getFatGoal());

        Map<String, String> vars = new HashMap<>();
        vars.put("user_name", user.getName());
        vars.put("daily_calorie_target", dailyCalorieTarget.toString());
        vars.put("protein_target", proteinTarget.toString());
        vars.put("carbs_target", carbsTarget.toString());
        vars.put("fat_target", fatTarget.toString());
        vars.put("estimated_days", String.valueOf(estimatedDays));
        vars.put("fat_goal", user.getFatGoal().name());
        vars.put("muscle_goal", user.getMuscleGoal().name());
        vars.put("intensity_level", user.getIntensityLevel().name());
        vars.put("activity_level", user.getActivityLevel().name());
        vars.put("workout_recommendation", workoutRec);

        String prompt = promptBuilder.build("onboarding-summary.txt", vars);
        try {
            String aiSummary = geminiAiService.callForText(prompt);
            if (aiSummary != null && !aiSummary.isBlank()) {
                return aiSummary;
            }
        } catch (Exception e) {
            log.error("Failed to generate AI onboarding summary", e);
        }

        return "🎉 You're all set! Based on your goals:\n" +
               "🔥 Daily Calories: " + dailyCalorieTarget + " kcal\n" +
               "🥩 Protein: " + proteinTarget + "g\n" +
               "🍚 Carbs: " + carbsTarget + "g\n" +
               "🥑 Fats: " + fatTarget + "g\n\n" +
               "Recommended Workout: " + workoutRec + ".\n" +
               "Estimated timeline to goal: " + estimatedDays + " days.\n" +
               "Start logging your food and workouts by just texting me what you did!";
    }
}
