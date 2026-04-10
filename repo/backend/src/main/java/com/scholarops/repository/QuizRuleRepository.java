package com.scholarops.repository;

import com.scholarops.model.entity.QuizRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRuleRepository extends JpaRepository<QuizRule, Long> {

    List<QuizRule> findByQuizPaperId(Long quizPaperId);
}
