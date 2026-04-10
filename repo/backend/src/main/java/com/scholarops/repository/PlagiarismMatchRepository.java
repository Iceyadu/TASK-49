package com.scholarops.repository;

import com.scholarops.model.entity.PlagiarismMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlagiarismMatchRepository extends JpaRepository<PlagiarismMatch, Long> {

    List<PlagiarismMatch> findByPlagiarismCheckId(Long plagiarismCheckId);
}
