package com.scholarops.repository;

import com.scholarops.model.entity.GradingState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface GradingStateRepository extends JpaRepository<GradingState, Long> {

    Page<GradingState> findByStatus(String status, Pageable pageable);

    Page<GradingState> findByAssignedToId(Long assignedToId, Pageable pageable);

    Optional<GradingState> findBySubmissionAnswerId(Long submissionAnswerId);

    @Query("SELECT COUNT(gs) > 0 FROM GradingState gs WHERE gs.submissionAnswer.submission.id = :submissionId AND gs.assignedTo.id = :userId")
    boolean existsBySubmissionIdAndAssignedToId(@Param("submissionId") Long submissionId, @Param("userId") Long userId);
}
