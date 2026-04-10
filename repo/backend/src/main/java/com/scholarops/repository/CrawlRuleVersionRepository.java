package com.scholarops.repository;

import com.scholarops.model.entity.CrawlRuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlRuleVersionRepository extends JpaRepository<CrawlRuleVersion, Long> {

    List<CrawlRuleVersion> findBySourceProfileId(Long sourceProfileId);

    List<CrawlRuleVersion> findBySourceProfileIdOrderByVersionNumberDesc(Long sourceProfileId);

    List<CrawlRuleVersion> findBySourceProfileIdAndIsActive(Long sourceProfileId, Boolean isActive);

    Optional<CrawlRuleVersion> findTopBySourceProfileIdOrderByVersionNumberDesc(Long sourceProfileId);

    Optional<CrawlRuleVersion> findBySourceProfileIdAndVersionNumber(Long sourceProfileId, Integer versionNumber);

    Optional<CrawlRuleVersion> findBySourceProfileIdAndIsActiveTrue(Long sourceProfileId);

    @Query("SELECT MAX(crv.versionNumber) FROM CrawlRuleVersion crv WHERE crv.sourceProfile.id = :sourceProfileId")
    Integer findMaxVersionBySourceProfileId(@Param("sourceProfileId") Long sourceProfileId);
}
