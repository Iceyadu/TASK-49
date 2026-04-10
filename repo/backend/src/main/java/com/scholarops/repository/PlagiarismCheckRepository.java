package com.scholarops.repository;

import com.scholarops.model.entity.PlagiarismCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlagiarismCheckRepository extends JpaRepository<PlagiarismCheck, Long> {

    List<PlagiarismCheck> findBySubmissionId(Long submissionId);

    List<PlagiarismCheck> findByIsFlagged(Boolean isFlagged);
}
