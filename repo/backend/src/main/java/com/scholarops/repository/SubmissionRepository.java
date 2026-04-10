package com.scholarops.repository;

import com.scholarops.model.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByStudentId(Long studentId);

    List<Submission> findByQuizPaperId(Long quizPaperId);

    List<Submission> findByQuizPaperIdAndStudentId(Long quizPaperId, Long studentId);

    long countByQuizPaperIdAndStudentId(Long quizPaperId, Long studentId);
}
