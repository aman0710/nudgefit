package com.nudgefit.service;

import com.nudgefit.ai.GeminiAiService;
import com.nudgefit.ai.PromptBuilder;
import com.nudgefit.model.dto.ParsedWorkoutResponse;
import com.nudgefit.model.entity.DailyLog;
import com.nudgefit.model.entity.User;
import com.nudgefit.model.entity.WorkoutEntry;
import com.nudgefit.repository.DailyLogRepository;
import com.nudgefit.repository.WorkoutEntryRepository;
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
public class WorkoutLoggingService {

    private final GeminiAiService geminiAiService;
    private final PromptBuilder promptBuilder;
    private final DailyLogRepository dailyLogRepository;
    private final WorkoutEntryRepository workoutEntryRepository;
    private final GoalEngineService goalEngineService;
    private final CoachResponseService coachResponseService;

    @Transactional
    public String logWorkout(User user, String userMessage) {
        String prompt = promptBuilder.buildPrompt("workout-parsing.txt", Map.of(
                "user_message", userMessage,
                "current_weight", String.valueOf(user.getCurrentWeightKg())
        ));

        ParsedWorkoutResponse response = geminiAiService.call(prompt, ParsedWorkoutResponse.class);

        if (response == null) {
            return "Sorry, I couldn't quite understand that workout. Can you try rephrasing?";
        }

        DailyLog dailyLog = dailyLogRepository.findByUserIdAndLogDate(user.getId(), LocalDate.now())
                .orElseGet(() -> createNewDailyLog(user));

        WorkoutEntry entry = WorkoutEntry.builder()
                .dailyLogId(dailyLog.getId())
                .userId(user.getId())
                .rawUserInput(userMessage)
                .workoutType(response.workout_type() != null ? response.workout_type().name() : "CARDIO")
                .durationMinutes(response.duration_minutes())
                .caloriesBurned(response.calories_burned())
                .loggedAt(LocalDateTime.now())
                // .details(serialize details to JSON if needed)
                .build();

        workoutEntryRepository.save(entry);

        // Update DailyLog
        dailyLog.setTotalCaloriesBurned(dailyLog.getTotalCaloriesBurned().add(response.calories_burned() != null ? response.calories_burned() : BigDecimal.ZERO));
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
                .targetProteinG(user.getDailyProteinTargetG())
                .targetCarbsG(user.getDailyCarbsTargetG())
                .targetFatG(user.getDailyFatTargetG())
                .build();
        return dailyLogRepository.save(log);
    }
}
