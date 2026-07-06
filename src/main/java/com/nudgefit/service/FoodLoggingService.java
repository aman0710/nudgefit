package com.nudgefit.service;

import com.nudgefit.ai.GeminiAiService;
import com.nudgefit.ai.PromptBuilder;
import com.nudgefit.model.dto.ParsedFoodResponse;
import com.nudgefit.model.entity.DailyLog;
import com.nudgefit.model.entity.FoodEntry;
import com.nudgefit.model.entity.User;
import com.nudgefit.repository.DailyLogRepository;
import com.nudgefit.repository.FoodEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodLoggingService {

    private final GeminiAiService geminiAiService;
    private final PromptBuilder promptBuilder;
    private final DailyLogRepository dailyLogRepository;
    private final FoodEntryRepository foodEntryRepository;
    private final GoalEngineService goalEngineService;
    private final CoachResponseService coachResponseService;

    @Transactional
    public String logFood(User user, String userMessage) {
        String prompt = promptBuilder.buildPrompt("food-parsing.txt", Map.of(
                "user_message", userMessage,
                "current_weight", String.valueOf(user.getCurrentWeightKg())
        ));

        ParsedFoodResponse response = geminiAiService.call(prompt, ParsedFoodResponse.class);

        if (response == null) {
            return "Sorry, I couldn't quite understand that meal. Can you try rephrasing?";
        }

        DailyLog dailyLog = dailyLogRepository.findByUserIdAndLogDate(user.getId(), LocalDate.now())
                .orElseGet(() -> createNewDailyLog(user));

        FoodEntry entry = FoodEntry.builder()
                .dailyLogId(dailyLog.getId())
                .userId(user.getId())
                .rawUserInput(userMessage)
                .totalCalories(response.total_calories())
                .proteinG(response.total_protein_g())
                .carbsG(response.total_carbs_g())
                .fatG(response.total_fat_g())
                .mealType(response.meal_type() != null ? response.meal_type().name() : "SNACK")
                .loggedAt(LocalDateTime.now())
                // .parsedItems(serialize items to JSON if needed)
                .build();

        foodEntryRepository.save(entry);

        // Update DailyLog
        dailyLog.setTotalCaloriesConsumed(dailyLog.getTotalCaloriesConsumed().add(response.total_calories() != null ? response.total_calories() : BigDecimal.ZERO));
        dailyLog.setNetCalories(dailyLog.getTotalCaloriesConsumed().subtract(dailyLog.getTotalCaloriesBurned()));
        dailyLogRepository.save(dailyLog);

        goalEngineService.recalculateGoalAndSnapshot(user, dailyLog);

        return coachResponseService.generateCoachingResponse(user, userMessage, dailyLog);
    }

    private DailyLog createNewDailyLog(User user) {
        DailyLog log = DailyLog.builder()
                .userId(user.getId())
                .logDate(LocalDate.now())
                .totalCaloriesConsumed(BigDecimal.ZERO)
                .totalCaloriesBurned(BigDecimal.ZERO)
                .netCalories(BigDecimal.ZERO)
                .targetCalories(user.getDailyCalorieTarget())
                .build();
        return dailyLogRepository.save(log);
    }
}
