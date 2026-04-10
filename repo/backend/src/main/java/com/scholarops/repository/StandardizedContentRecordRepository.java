package com.scholarops.repository;

import com.scholarops.model.entity.StandardizedContentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StandardizedContentRecordRepository extends JpaRepository<StandardizedContentRecord, Long> {

    List<StandardizedContentRecord> findByIsPublished(Boolean isPublished);

    @Query(value = "SELECT * FROM standardized_content_records " +
                   "WHERE MATCH(title, description, body_text) AGAINST(:searchTerm IN NATURAL LANGUAGE MODE) " +
                   "AND is_published = true",
           countQuery = "SELECT COUNT(*) FROM standardized_content_records " +
                        "WHERE MATCH(title, description, body_text) AGAINST(:searchTerm IN NATURAL LANGUAGE MODE) " +
                        "AND is_published = true",
           nativeQuery = true)
    Page<StandardizedContentRecord> searchCatalog(@Param("searchTerm") String searchTerm, Pageable pageable);

    Page<StandardizedContentRecord> findBySourceProfileId(Long sourceProfileId, Pageable pageable);

    List<StandardizedContentRecord> findByIsPublishedTrue();
}
