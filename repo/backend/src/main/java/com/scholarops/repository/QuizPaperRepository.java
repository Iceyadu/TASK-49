package com.scholarops.repository;

import com.scholarops.model.entity.QuizPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizPaperRepository extends JpaRepository<QuizPaper, Long> {

    List<QuizPaper> findByCreatedById(Long createdById);

    List<QuizPaper> findByIsPublished(Boolean isPublished);

    List<QuizPaper> findByQuestionBankId(Long questionBankId);
}
