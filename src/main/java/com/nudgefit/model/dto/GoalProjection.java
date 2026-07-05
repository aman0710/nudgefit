package com.nudgefit.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalProjection(
        BigDecimal daysImpact,
        LocalDate newProjectedDate,
        BigDecimal todayNet,
        BigDecimal remainingBudget
) {}
