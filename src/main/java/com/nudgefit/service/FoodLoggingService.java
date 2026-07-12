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
import com.nudgefit.model.enums.MealType;
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
    private final CoachResponseService coachResponseService;

    @Transactional
    public String logFood(User user, String userMessage, MealType mealType) {
        String mealTypeStr = mealType != null ? mealType.name() : "SNACK";

        String prompt = promptBuilder.build("food-parsing.txt", Map.of(
                "user_message", userMessage,
                "current_weight", String.valueOf(user.getCurrentWeightKg()),
                "meal_type", mealTypeStr
        ));

        ParsedFoodResponse response = geminiAiService.call(prompt, ParsedFoodResponse.class);

        if (response == null) {
            return "Sorry, I couldn't quite understand that meal. Can you try rephrasing? 🤔";
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
                .mealType(response.meal_type() != null ? response.meal_type() : (mealType != null ? mealType : MealType.SNACK))
                .loggedAt(LocalDateTime.now())
                .build();

        foodEntryRepository.save(entry);

        // Update DailyLog totals
        dailyLog.setTotalCaloriesConsumed(dailyLog.getTotalCaloriesConsumed().add(response.total_calories() != null ? response.total_calories() : BigDecimal.ZERO));
        dailyLog.setTotalProteinConsumed(dailyLog.getTotalProteinConsumed().add(response.total_protein_g() != null ? response.total_protein_g() : BigDecimal.ZERO));
        dailyLog.setTotalCarbsConsumed(dailyLog.getTotalCarbsConsumed().add(response.total_carbs_g() != null ? response.total_carbs_g() : BigDecimal.ZERO));
        dailyLog.setTotalFatConsumed(dailyLog.getTotalFatConsumed().add(response.total_fat_g() != null ? response.total_fat_g() : BigDecimal.ZERO));
        dailyLog.setNetCalories(dailyLog.getTotalCaloriesConsumed().subtract(dailyLog.getTotalCaloriesBurned()));
        dailyLogRepository.save(dailyLog);

        return coachResponseService.generateCoachingResponse(user, userMessage, dailyLog, entry, null);
    }

    private DailyLog createNewDailyLog(User user) {
        DailyLog log = DailyLog.builder()
                .userId(user.getId())
                .logDate(LocalDate.now())
                .totalCaloriesConsumed(BigDecimal.ZERO)
                .totalCaloriesBurned(BigDecimal.ZERO)
                .netCalories(BigDecimal.ZERO)
                .totalProteinConsumed(BigDecimal.ZERO)
                .totalCarbsConsumed(BigDecimal.ZERO)
                .totalFatConsumed(BigDecimal.ZERO)
                .targetCalories(user.getDailyCalorieTarget())
                .build();
        return dailyLogRepository.save(log);
    }
}
