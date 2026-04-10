package com.scholarops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.CrawlRuleRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.repository.CrawlRuleVersionRepository;
import com.scholarops.repository.CrawlSourceProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlRuleServiceTest {

    @Mock private CrawlRuleVersionRepository crawlRuleVersionRepository;
    @Mock private CrawlSourceProfileRepository crawlSourceProfileRepository;
    @Mock private ParsingService parsingService;
    @Mock private AuditLogService auditLogService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CrawlRuleService crawlRuleService;

    @Test
    void testCreateRule() {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").build();

        CrawlRuleRequest request = new CrawlRuleRequest();
        request.setExtractionMethod("CSS_SELECTOR");
        request.setRuleDefinition(Map.of("title", "h1.title"));
        request.setFieldMappings(Map.of("title", "articleTitle"));
        request.setNotes("Initial rule");

        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findMaxVersionBySourceProfileId(1L)).thenReturn(null);
        when(crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(1L))
                .thenReturn(Optional.empty());
        when(crawlRuleVersionRepository.save(any(CrawlRuleVersion.class)))
                .thenAnswer(inv -> {
                    CrawlRuleVersion v = inv.getArgument(0);
                    v.setId(10L);
                    return v;
                });

        CrawlRuleVersion result = crawlRuleService.createRule(1L, request, 99L);

        assertNotNull(result);
        assertEquals(1, result.getVersionNumber());
        assertTrue(result.getIsActive());
        assertEquals("CSS_SELECTOR", result.getExtractionMethod());
    }

    @Test
    void testHotUpdate() {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").build();

        CrawlRuleVersion existingRule = CrawlRuleVersion.builder()
                .id(10L).sourceProfile(source).versionNumber(1)
                .extractionMethod("CSS_SELECTOR").isActive(true).build();

        CrawlRuleRequest request = new CrawlRuleRequest();
        request.setExtractionMethod("REGEX");
        request.setRuleDefinition(Map.of("title", "pattern.*"));

        when(crawlRuleVersionRepository.findById(10L)).thenReturn(Optional.of(existingRule));
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findMaxVersionBySourceProfileId(1L)).thenReturn(1);
        when(crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(existingRule));
        when(crawlRuleVersionRepository.save(any(CrawlRuleVersion.class)))
                .thenAnswer(inv -> {
                    CrawlRuleVersion v = inv.getArgument(0);
                    v.setId(11L);
                    return v;
                });

        CrawlRuleVersion result = crawlRuleService.hotUpdate(10L, request, 99L);

        assertNotNull(result);
        assertEquals(2, result.getVersionNumber());
    }

    @Test
    void testRevertToVersion() {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").build();

        CrawlRuleVersion currentRule = CrawlRuleVersion.builder()
                .id(12L).sourceProfile(source).versionNumber(3)
                .extractionMethod("REGEX").isActive(true).build();

        CrawlRuleVersion targetVersion = CrawlRuleVersion.builder()
                .id(10L).sourceProfile(source).versionNumber(1)
                .extractionMethod("CSS_SELECTOR")
                .ruleDefinition("{\"title\":\"h1\"}")
                .fieldMappings("{\"title\":\"title\"}")
                .isActive(false).build();

        when(crawlRuleVersionRepository.findById(12L)).thenReturn(Optional.of(currentRule));
        when(crawlRuleVersionRepository.findById(10L)).thenReturn(Optional.of(targetVersion));
        when(crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(currentRule));
        when(crawlRuleVersionRepository.findMaxVersionBySourceProfileId(1L)).thenReturn(3);
        when(crawlRuleVersionRepository.save(any(CrawlRuleVersion.class)))
                .thenAnswer(inv -> {
                    CrawlRuleVersion v = inv.getArgument(0);
                    v.setId(13L);
                    return v;
                });

        CrawlRuleVersion result = crawlRuleService.revertToVersion(12L, 10L, 99L);

        assertNotNull(result);
        assertEquals(4, result.getVersionNumber());
        assertEquals("CSS_SELECTOR", result.getExtractionMethod());
        assertTrue(result.getIsActive());
        assertTrue(result.getNotes().contains("Reverted to version 1"));
    }

    @Test
    void testVersionNumberIncrement() {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").build();

        CrawlRuleRequest request = new CrawlRuleRequest();
        request.setExtractionMethod("CSS_SELECTOR");
        request.setRuleDefinition(Map.of("title", "h1"));

        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findMaxVersionBySourceProfileId(1L)).thenReturn(5);
        when(crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(1L))
                .thenReturn(Optional.empty());
        when(crawlRuleVersionRepository.save(any(CrawlRuleVersion.class)))
                .thenAnswer(inv -> {
                    CrawlRuleVersion v = inv.getArgument(0);
                    v.setId(20L);
                    return v;
                });

        CrawlRuleVersion result = crawlRuleService.createRule(1L, request, 99L);

        assertEquals(6, result.getVersionNumber());
    }
}
