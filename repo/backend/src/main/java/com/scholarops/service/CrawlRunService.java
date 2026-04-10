package com.scholarops.service;

import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.crawler.CrawlerEngine;
import com.scholarops.model.dto.CrawlRunRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlRun;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.User;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.CrawlRunRepository;
import com.scholarops.repository.CrawlRuleVersionRepository;
import com.scholarops.repository.CrawlSourceProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CrawlRunService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlRunService.class);

    private final CrawlRunRepository crawlRunRepository;
    private final CrawlSourceProfileRepository crawlSourceProfileRepository;
    private final CrawlRuleVersionRepository crawlRuleVersionRepository;
    private final AuditLogService auditLogService;
    private final CrawlerEngine crawlerEngine;

    public CrawlRunService(CrawlRunRepository crawlRunRepository,
                            CrawlSourceProfileRepository crawlSourceProfileRepository,
                            CrawlRuleVersionRepository crawlRuleVersionRepository,
                            AuditLogService auditLogService,
                            CrawlerEngine crawlerEngine) {
        this.crawlRunRepository = crawlRunRepository;
        this.crawlSourceProfileRepository = crawlSourceProfileRepository;
        this.crawlRuleVersionRepository = crawlRuleVersionRepository;
        this.auditLogService = auditLogService;
        this.crawlerEngine = crawlerEngine;
    }

    @Transactional
    public CrawlRun startRun(CrawlRunRequest request, Long userId) {
        CrawlSourceProfile sourceProfile = crawlSourceProfileRepository.findById(request.getSourceProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("CrawlSourceProfile", "id", request.getSourceProfileId()));

        CrawlRuleVersion ruleVersion = crawlRuleVersionRepository.findById(request.getRuleVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRuleVersion", "id", request.getRuleVersionId()));

        CrawlRun run = CrawlRun.builder()
                .sourceProfile(sourceProfile)
                .ruleVersion(ruleVersion)
                .status("PENDING")
                .totalPages(0)
                .pagesCrawled(0)
                .pagesFailed(0)
                .itemsExtracted(0)
                .initiatedBy(User.builder().id(userId).build())
                .build();

        CrawlRun savedRun = crawlRunRepository.save(run);

        // Launch real async crawl through crawler engine.
        crawlerEngine.executeCrawl(savedRun, sourceProfile, ruleVersion);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_RUN_START,
                "CrawlRun",
                savedRun.getId(),
                "Started crawl run for source: " + sourceProfile.getName(),
                null,
                null
        );

        logger.info("Crawl run {} started for source '{}' by userId={}", savedRun.getId(), sourceProfile.getName(), userId);
        return savedRun;
    }

    @Transactional(readOnly = true)
    public CrawlRun getRun(Long id) {
        return crawlRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRun", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<CrawlRun> listRuns(Pageable pageable, Long sourceId) {
        if (sourceId != null) {
            return crawlRunRepository.findBySourceProfileId(sourceId, pageable);
        }
        return crawlRunRepository.findAll(pageable);
    }

    @Transactional
    public CrawlRun cancelRun(Long id, Long userId) {
        CrawlRun run = crawlRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRun", "id", id));

        if ("COMPLETED".equals(run.getStatus()) || "CANCELLED".equals(run.getStatus()) || "FAILED".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot cancel a run that is already " + run.getStatus());
        }

        run.setStatus("CANCELLED");
        run.setCompletedAt(LocalDateTime.now());
        CrawlRun cancelled = crawlRunRepository.save(run);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_RUN_CANCEL,
                "CrawlRun",
                id,
                "Cancelled crawl run",
                null,
                null
        );

        logger.info("Crawl run {} cancelled by userId={}", id, userId);
        return cancelled;
    }
}
