package com.scholarops.repository;

import com.scholarops.model.entity.WrongAnswerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WrongAnswerHistoryRepository extends JpaRepository<WrongAnswerHistory, Long> {

    List<WrongAnswerHistory> findByStudentId(Long studentId);

    List<WrongAnswerHistory> findByStudentIdAndQuestionId(Long studentId, Long questionId);

    List<WrongAnswerHistory> findByStudentIdAndReviewed(Long studentId, Boolean reviewed);
}
