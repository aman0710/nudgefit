package com.nudgefit.service;

import com.nudgefit.ai.GeminiAiService;
import com.nudgefit.ai.PromptBuilder;
import com.nudgefit.model.entity.DailyLog;
import com.nudgefit.model.entity.User;
import com.nudgefit.util.MacroCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.nudgefit.model.entity.FoodEntry;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachResponseService {

    private final GeminiAiService geminiAiService;
    private final PromptBuilder promptBuilder;
    private final ConversationContextService contextService;

    public String generateCoachingResponse(User user, String userMessage, DailyLog dailyLog, FoodEntry currentFoodEntry) {
        // Fetch context
        List<String> recentMessages = contextService.getRecentMessages(user.getPhoneNumber(), 5);
        String conversationHistory = String.join("\n", recentMessages);

        int estimatedDays = MacroCalculator.calculateEstimatedTimelineDays(
            user.getCurrentWeightKg(), user.getCurrentBodyFatPct(), user.getTargetBodyFatPct(),
            user.getCurrentMuscleMassKg(), user.getTargetMuscleMassKg(),
            user.getFatGoal(), user.getMuscleGoal(), user.getIntensityLevel()
        );

        Map<String, String> variables = new HashMap<>();
        variables.put("user_name", user.getName());
        variables.put("current_weight", String.valueOf(user.getCurrentWeightKg()));
        variables.put("fat_goal", user.getFatGoal() != null ? user.getFatGoal().name() : "N/A");
        variables.put("muscle_goal", user.getMuscleGoal() != null ? user.getMuscleGoal().name() : "N/A");
        variables.put("intensity_level", user.getIntensityLevel() != null ? user.getIntensityLevel().name() : "N/A");
        variables.put("current_body_fat_pct", String.valueOf(user.getCurrentBodyFatPct()));
        variables.put("target_body_fat_pct", String.valueOf(user.getTargetBodyFatPct()));
        variables.put("current_muscle_mass_kg", String.valueOf(user.getCurrentMuscleMassKg()));
        variables.put("target_muscle_mass_kg", String.valueOf(user.getTargetMuscleMassKg()));
        
        // Workout Recommendation
        String workoutRec = MacroCalculator.calculateWorkoutRecommendations(user.getIntensityLevel(), user.getMuscleGoal(), user.getFatGoal());
        variables.put("workout_recommendation", workoutRec);

        // Log variables
        if (dailyLog != null) {
            variables.put("calories_consumed", String.valueOf(dailyLog.getTotalCaloriesConsumed()));
            variables.put("calories_burned", String.valueOf(dailyLog.getTotalCaloriesBurned()));
            variables.put("net_calories", String.valueOf(dailyLog.getNetCalories()));
            variables.put("target_calories", String.valueOf(dailyLog.getTargetCalories()));
            
            variables.put("protein_consumed", String.valueOf(dailyLog.getTotalProteinConsumed()));
            variables.put("carbs_consumed", String.valueOf(dailyLog.getTotalCarbsConsumed()));
            variables.put("fats_consumed", String.valueOf(dailyLog.getTotalFatConsumed()));
            
            variables.put("target_protein_g", String.valueOf(user.getDailyProteinTargetG()));
            variables.put("target_carbs_g", String.valueOf(user.getDailyCarbsTargetG()));
            variables.put("target_fat_g", String.valueOf(user.getDailyFatTargetG()));
        } else {
            variables.put("calories_consumed", "0");
            variables.put("calories_burned", "0");
            variables.put("net_calories", "0");
            variables.put("target_calories", String.valueOf(user.getDailyCalorieTarget()));
            variables.put("protein_consumed", "0");
            variables.put("carbs_consumed", "0");
            variables.put("fats_consumed", "0");
            variables.put("target_protein_g", String.valueOf(user.getDailyProteinTargetG()));
            variables.put("target_carbs_g", String.valueOf(user.getDailyCarbsTargetG()));
            variables.put("target_fat_g", String.valueOf(user.getDailyFatTargetG()));
        }

        if (currentFoodEntry != null) {
            String mealMacros = String.format("Calories: %s, Protein: %sg, Carbs: %sg, Fat: %sg",
                    currentFoodEntry.getTotalCalories(),
                    currentFoodEntry.getProteinG(),
                    currentFoodEntry.getCarbsG(),
                    currentFoodEntry.getFatG());
            variables.put("current_meal_macros", mealMacros);
        } else {
            variables.put("current_meal_macros", "N/A");
        }

        variables.put("on_track_days", "5"); // Mocked for now
        variables.put("user_message", userMessage);
        variables.put("conversation_history", conversationHistory);

        String prompt = promptBuilder.build("coaching-response.txt", variables);

        try {
            String response = geminiAiService.callForText(prompt);
            if (response != null && !response.isBlank()) {
                return response;
            }
            return "You're doing great! Keep hitting those macros! 💪";
        } catch (Exception e) {
            log.error("Failed to generate coaching response for {}: {}", user.getPhoneNumber(), e.getMessage());
            return "You're doing great! Keep it up! 💪";
        }
    }
}
