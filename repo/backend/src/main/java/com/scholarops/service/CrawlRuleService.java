package com.scholarops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.CrawlRuleRequest;
import com.scholarops.model.dto.ExtractionTestRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.User;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.CrawlRuleVersionRepository;
import com.scholarops.repository.CrawlSourceProfileRepository;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrawlRuleService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlRuleService.class);

    private final CrawlRuleVersionRepository crawlRuleVersionRepository;
    private final CrawlSourceProfileRepository crawlSourceProfileRepository;
    private final ParsingService parsingService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public CrawlRuleService(CrawlRuleVersionRepository crawlRuleVersionRepository,
                             CrawlSourceProfileRepository crawlSourceProfileRepository,
                             ParsingService parsingService,
                             AuditLogService auditLogService,
                             ObjectMapper objectMapper) {
        this.crawlRuleVersionRepository = crawlRuleVersionRepository;
        this.crawlSourceProfileRepository = crawlSourceProfileRepository;
        this.parsingService = parsingService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CrawlRuleVersion createRule(Long sourceId, CrawlRuleRequest request, Long userId) {
        CrawlSourceProfile source = crawlSourceProfileRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlSourceProfile", "id", sourceId));

        // Determine the next version number
        Integer maxVersion = crawlRuleVersionRepository.findMaxVersionBySourceProfileId(sourceId);
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // Deactivate current active version
        crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(sourceId)
                .ifPresent(active -> {
                    active.setIsActive(false);
                    crawlRuleVersionRepository.save(active);
                });

        CrawlRuleVersion ruleVersion = CrawlRuleVersion.builder()
                .sourceProfile(source)
                .versionNumber(nextVersion)
                .extractionMethod(request.getExtractionMethod())
                .ruleDefinition(toJson(request.getRuleDefinition()))
                .fieldMappings(toJson(request.getFieldMappings()))
                .typeValidations(request.getTypeValidations() != null ? toJson(request.getTypeValidations()) : null)
                .isActive(true)
                .createdBy(User.builder().id(userId).build())
                .notes(request.getNotes())
                .build();

        CrawlRuleVersion saved = crawlRuleVersionRepository.save(ruleVersion);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_RULE_CREATE,
                "CrawlRuleVersion",
                saved.getId(),
                String.format("Created rule v%d for source '%s'", nextVersion, source.getName()),
                null,
                null
        );

        logger.info("Crawl rule v{} created for sourceId={} by userId={}", nextVersion, sourceId, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CrawlRuleVersion> getRulesForSource(Long sourceId) {
        return crawlRuleVersionRepository.findBySourceProfileIdOrderByVersionNumberDesc(sourceId);
    }

    @Transactional(readOnly = true)
    public CrawlRuleVersion getActiveRule(Long sourceId) {
        return crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active crawl rule found for source with id: " + sourceId));
    }

    @Transactional(readOnly = true)
    public CrawlRuleVersion getRule(Long id) {
        return crawlRuleVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRuleVersion", "id", id));
    }

    @Transactional
    public CrawlRuleVersion hotUpdate(Long ruleId, CrawlRuleRequest request, Long userId) {
        CrawlRuleVersion existingRule = crawlRuleVersionRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRuleVersion", "id", ruleId));

        Long sourceId = existingRule.getSourceProfile().getId();

        // Create a new version using the updated request
        return createRule(sourceId, request, userId);
    }

    @Transactional
    public CrawlRuleVersion revertToVersion(Long ruleId, Long targetVersionId, Long userId) {
        CrawlRuleVersion currentRule = crawlRuleVersionRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRuleVersion", "id", ruleId));

        CrawlRuleVersion targetVersion = crawlRuleVersionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlRuleVersion", "id", targetVersionId));

        Long sourceId = currentRule.getSourceProfile().getId();

        // Verify both rules belong to the same source
        if (!targetVersion.getSourceProfile().getId().equals(sourceId)) {
            throw new IllegalArgumentException("Target version does not belong to the same source");
        }

        // Deactivate current active version
        crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(sourceId)
                .ifPresent(active -> {
                    active.setIsActive(false);
                    crawlRuleVersionRepository.save(active);
                });

        // Clone target version as a new version
        Integer maxVersion = crawlRuleVersionRepository.findMaxVersionBySourceProfileId(sourceId);
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        CrawlRuleVersion revertedVersion = CrawlRuleVersion.builder()
                .sourceProfile(currentRule.getSourceProfile())
                .versionNumber(nextVersion)
                .extractionMethod(targetVersion.getExtractionMethod())
                .ruleDefinition(targetVersion.getRuleDefinition())
                .fieldMappings(targetVersion.getFieldMappings())
                .typeValidations(targetVersion.getTypeValidations())
                .isActive(true)
                .createdBy(User.builder().id(userId).build())
                .notes("Reverted to version " + targetVersion.getVersionNumber())
                .build();

        CrawlRuleVersion saved = crawlRuleVersionRepository.save(revertedVersion);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_RULE_REVERT,
                "CrawlRuleVersion",
                saved.getId(),
                String.format("Reverted source %d to version %d (cloned as v%d)",
                        sourceId, targetVersion.getVersionNumber(), nextVersion),
                null,
                null
        );

        logger.info("Crawl rule reverted for sourceId={}: v{} -> v{} (cloned from v{})",
                sourceId, currentRule.getVersionNumber(), nextVersion, targetVersion.getVersionNumber());
        return saved;
    }

    /**
     * Tests extraction on a sample content using the given rule definition.
     * Returns a preview map of field -> extracted values.
     */
    public Map<String, List<String>> testExtraction(ExtractionTestRequest request) {
        String sampleContent = fetchSampleContent(request.getSampleUrl());
        String method = request.getExtractionMethod();
        Map<String, Object> ruleDefinition = request.getRuleDefinition();
        Map<String, String> fieldMappings = request.getFieldMappings();

        Map<String, List<String>> preview = new HashMap<>();

        if (ruleDefinition == null || ruleDefinition.isEmpty()) {
            throw new IllegalArgumentException("Rule definition must not be empty");
        }

        // For each rule in the definition, run extraction
        for (Map.Entry<String, Object> entry : ruleDefinition.entrySet()) {
            String fieldName = entry.getKey();
            String expression = entry.getValue().toString();

            List<String> extracted = parsingService.extract(sampleContent, method, expression);

            // Apply field mapping if available
            String mappedField = (fieldMappings != null && fieldMappings.containsKey(fieldName))
                    ? fieldMappings.get(fieldName)
                    : fieldName;

            preview.put(mappedField, extracted);
        }

        return preview;
    }

    private String fetchSampleContent(String sampleUrl) {
        if (sampleUrl == null || sampleUrl.isBlank()) {
            throw new IllegalArgumentException("Sample URL must not be empty");
        }
        try {
            return Jsoup.connect(sampleUrl)
                    .timeout(15000)
                    .userAgent("ScholarOps-Crawler/1.0")
                    .ignoreContentType(true)
                    .execute()
                    .body();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to fetch sample URL: " + sampleUrl, e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize to JSON", e);
        }
    }
}
