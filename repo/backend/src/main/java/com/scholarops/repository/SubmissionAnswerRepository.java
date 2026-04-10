package com.scholarops.repository;

import com.scholarops.model.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, Long> {

    List<SubmissionAnswer> findBySubmissionId(Long submissionId);

    Optional<SubmissionAnswer> findBySubmissionIdAndQuestionId(Long submissionId, Long questionId);
}
