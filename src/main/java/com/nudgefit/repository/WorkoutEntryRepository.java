package com.nudgefit.repository;

import com.nudgefit.model.entity.WorkoutEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkoutEntryRepository extends JpaRepository<WorkoutEntry, UUID> {

    List<WorkoutEntry> findByDailyLogId(UUID dailyLogId);
}
