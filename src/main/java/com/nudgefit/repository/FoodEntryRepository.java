package com.nudgefit.repository;

import com.nudgefit.model.entity.FoodEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FoodEntryRepository extends JpaRepository<FoodEntry, UUID> {

    List<FoodEntry> findByDailyLogId(UUID dailyLogId);

    List<FoodEntry> findByUserIdAndLoggedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
}
