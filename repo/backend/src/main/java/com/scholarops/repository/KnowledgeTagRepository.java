package com.scholarops.repository;

import com.scholarops.model.entity.KnowledgeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeTagRepository extends JpaRepository<KnowledgeTag, Long> {

    Optional<KnowledgeTag> findByName(String name);

    List<KnowledgeTag> findByCategory(String category);
}
