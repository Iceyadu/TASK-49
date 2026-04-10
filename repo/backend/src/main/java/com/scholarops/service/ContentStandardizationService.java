package com.scholarops.service;

import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import com.scholarops.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ContentStandardizationService {
    private static final Logger logger = LoggerFactory.getLogger(ContentStandardizationService.class);
    private final StandardizedContentRecordRepository contentRepository;
    private final MediaMetadataRepository mediaMetadataRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ContentStandardizationService(StandardizedContentRecordRepository contentRepository,
            MediaMetadataRepository mediaMetadataRepository, UserRepository userRepository,
            AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.contentRepository = contentRepository;
        this.mediaMetadataRepository = mediaMetadataRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StandardizedContentRecord standardize(Map<String, Object> rawData, String timezoneId,
            CrawlRun crawlRun, CrawlSourceProfile source) {
        StandardizedContentRecord record = new StandardizedContentRecord();
        record.setCrawlRun(crawlRun);
        record.setSourceProfile(source);
        record.setTitle(getStringValue(rawData, "title", "Untitled"));
        record.setDescription(getStringValue(rawData, "description", null));
        record.setBodyText(getStringValue(rawData, "bodyText", null));
        record.setContentType(getStringValue(rawData, "contentType", "article"));
        record.setSourceUrl(getStringValue(rawData, "sourceUrl", null));

        String rawTimestamp = getStringValue(rawData, "timestamp", null);
        if (rawTimestamp != null && timezoneId != null) {
            record.setStandardizedTimestamp(TimestampNormalizer.normalize(rawTimestamp, timezoneId));
            record.setTimezoneId(timezoneId);
        }

        String rawLocation = getStringValue(rawData, "location", null);
        if (rawLocation != null) {
            record.setOriginalLocation(rawLocation);
            record.setNormalizedAddress(AddressNormalizer.normalize(rawLocation));
        }

        String textForLang = record.getBodyText() != null ? record.getBodyText() : record.getTitle();
        record.setDetectedLanguage(LanguageDetector.detect(textForLang));

        if (rawData.containsKey("price")) {
            try {
                record.setPrice(new java.math.BigDecimal(rawData.get("price").toString()));
            } catch (NumberFormatException ignored) {}
        }

        return contentRepository.save(record);
    }

    public Page<StandardizedContentRecord> listContent(Pageable pageable) {
        return contentRepository.findAll(pageable);
    }

    public StandardizedContentRecord getContentById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new com.scholarops.exception.ResourceNotFoundException("Content not found: " + id));
    }

    public List<MediaMetadata> getMediaMetadata(Long contentId) {
        return mediaMetadataRepository.findByContentRecordId(contentId);
    }

    @Transactional
    public void publishContent(List<Long> contentIds, Long userId) {
        User publisher = userRepository.findById(userId).orElseThrow();
        for (Long id : contentIds) {
            StandardizedContentRecord record = contentRepository.findById(id).orElseThrow(
                    () -> new com.scholarops.exception.ResourceNotFoundException("Content not found: " + id));
            record.setIsPublished(true);
            record.setPublishedAt(LocalDateTime.now());
            record.setPublishedBy(publisher);
            contentRepository.save(record);
            auditLogService.log(userId, AuditAction.CONTENT_PUBLISH, "StandardizedContentRecord", id,
                    "Published content: " + record.getTitle(), null, null);
        }
    }

    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
