package com.nudgefit.repository;

import com.nudgefit.model.entity.GoalSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalSnapshotRepository extends JpaRepository<GoalSnapshot, UUID> {

    Optional<GoalSnapshot> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    List<GoalSnapshot> findByUserIdAndSnapshotDateBetween(UUID userId, LocalDate start, LocalDate end);
}
