package com.scholarops.repository;

import com.scholarops.model.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    List<QuestionBank> findByCreatedById(Long createdById);
}
