package com.scholarops.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.CrawlRuleRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.repository.CrawlRuleVersionRepository;
import com.scholarops.repository.CrawlSourceProfileRepository;
import com.scholarops.service.AuditLogService;
import com.scholarops.service.CrawlRuleService;
import com.scholarops.service.ParsingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class CrawlWorkflowIntegrationTest {

    @MockBean private CrawlRuleVersionRepository crawlRuleVersionRepository;
    @MockBean private CrawlSourceProfileRepository crawlSourceProfileRepository;
    @MockBean private AuditLogService auditLogService;

    @Autowired private CrawlRuleService crawlRuleService;

    @Test
    void testCrawlRuleCreationAndVersioning() {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").baseUrl("https://example.com").build();

        // Setup mocks for first rule creation
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findMaxVersionBySourceProfileId(1L))
                .thenReturn(null)
                .thenReturn(1);
        when(crawlRuleVersionRepository.findBySourceProfileIdAndIsActiveTrue(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(crawlRuleVersionRepository.save(any(CrawlRuleVersion.class)))
                .thenAnswer(inv -> {
                    CrawlRuleVersion v = inv.getArgument(0);
                    if (v.getId() == null) {
                        v.setId((long)(Math.random() * 1000 + 100));
                    }
                    return v;
                });

        // Create first rule version
        CrawlRuleRequest request1 = new CrawlRuleRequest();
        request1.setExtractionMethod("CSS_SELECTOR");
        request1.setRuleDefinition(Map.of("title", "h1.article-title"));
        request1.setFieldMappings(Map.of("title", "articleTitle"));
        request1.setNotes("Version 1");

        CrawlRuleVersion v1 = crawlRuleService.createRule(1L, request1, 99L);

        assertNotNull(v1);
        assertEquals(1, v1.getVersionNumber());
        assertTrue(v1.getIsActive());

        // Create second rule version (hot update)
        CrawlRuleRequest request2 = new CrawlRuleRequest();
        request2.setExtractionMethod("REGEX");
        request2.setRuleDefinition(Map.of("title", "<h1>(.*?)</h1>"));
        request2.setNotes("Version 2 - switched to regex");

        CrawlRuleVersion v2 = crawlRuleService.createRule(1L, request2, 99L);

        assertNotNull(v2);
        assertEquals(2, v2.getVersionNumber());
        assertTrue(v2.getIsActive());

        // Verify audit logging was called for both creations
        verify(auditLogService, times(2)).log(
                eq(99L), any(), eq("CrawlRuleVersion"), any(), any(), any(), any());
    }
}
