package com.scholarops.repository;

import com.scholarops.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuestionBankId(Long questionBankId);

    List<Question> findByDifficultyLevel(Integer difficultyLevel);

    List<Question> findByQuestionBankIdAndDifficultyLevel(Long questionBankId, Integer difficultyLevel);

    long countByQuestionBankIdAndDifficultyLevel(Long questionBankId, Integer difficultyLevel);
}
