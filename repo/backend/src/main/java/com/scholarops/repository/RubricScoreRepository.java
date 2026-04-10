package com.scholarops.repository;

import com.scholarops.model.entity.RubricScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubricScoreRepository extends JpaRepository<RubricScore, Long> {

    List<RubricScore> findByGradingStateId(Long gradingStateId);
}
