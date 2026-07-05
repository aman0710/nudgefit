package com.nudgefit.service;

import com.nudgefit.model.entity.DailyLog;
import com.nudgefit.model.entity.GoalSnapshot;
import com.nudgefit.model.entity.User;
import com.nudgefit.repository.DailyLogRepository;
import com.nudgefit.repository.GoalSnapshotRepository;
import com.nudgefit.util.MacroCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalEngineService {

    private final DailyLogRepository dailyLogRepository;
    private final GoalSnapshotRepository goalSnapshotRepository;

    @Transactional
    public void recalculateGoalAndSnapshot(User user, DailyLog dailyLog) {
        // Calculate estimated timeline
        int estimatedDays = MacroCalculator.calculateEstimatedTimelineDays(
                user.getCurrentWeightKg(),
                user.getCurrentBodyFatPct(),
                user.getTargetBodyFatPct(),
                user.getCurrentMuscleMassKg(),
                user.getTargetMuscleMassKg(),
                user.getFatGoal(),
                user.getMuscleGoal(),
                user.getIntensityLevel()
        );

        // Record a snapshot
        GoalSnapshot snapshot = GoalSnapshot.builder()
                .userId(user.getId())
                .snapshotDate(LocalDate.now())
                .dailyCalorieTarget(user.getDailyCalorieTarget())
                .daysRemaining(estimatedDays)
                .daysImpact(BigDecimal.ZERO) // Removed goal deadline, impact doesn't strictly apply unless we calculate deviation
                .reason("Daily log update")
                .build();

        goalSnapshotRepository.save(snapshot);
    }
}
