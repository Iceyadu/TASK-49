package com.scholarops.repository;

import com.scholarops.model.entity.CrawlRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlRunRepository extends JpaRepository<CrawlRun, Long> {

    Page<CrawlRun> findBySourceProfileId(Long sourceProfileId, Pageable pageable);

    Page<CrawlRun> findByStatus(String status, Pageable pageable);
}
