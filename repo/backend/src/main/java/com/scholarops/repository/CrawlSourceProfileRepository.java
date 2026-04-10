package com.scholarops.repository;

import com.scholarops.model.entity.CrawlSourceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlSourceProfileRepository extends JpaRepository<CrawlSourceProfile, Long> {

    List<CrawlSourceProfile> findByCreatedById(Long createdById);

    List<CrawlSourceProfile> findByEnabled(Boolean enabled);
}
