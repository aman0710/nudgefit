package com.nudgefit.repository;

import com.nudgefit.model.entity.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, UUID> {

    Optional<DailyLog> findByUserIdAndLogDate(UUID userId, LocalDate date);

    int countByUserIdAndLogDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
