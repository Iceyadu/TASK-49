package com.scholarops.service;

import com.scholarops.crawler.CrawlerEngine;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.CrawlRunRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlRun;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.repository.CrawlRunRepository;
import com.scholarops.repository.CrawlRuleVersionRepository;
import com.scholarops.repository.CrawlSourceProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlRunServiceTest {

    @Mock private CrawlRunRepository crawlRunRepository;
    @Mock private CrawlSourceProfileRepository crawlSourceProfileRepository;
    @Mock private CrawlRuleVersionRepository crawlRuleVersionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CrawlerEngine crawlerEngine;

    @InjectMocks
    private CrawlRunService crawlRunService;

    private CrawlSourceProfile buildSource(Long id) {
        return CrawlSourceProfile.builder().id(id).name("source-" + id)
                .baseUrl("https://example.org/" + id).enabled(true).build();
    }

    private CrawlRuleVersion buildRuleVersion(Long id, CrawlSourceProfile source) {
        return CrawlRuleVersion.builder().id(id).sourceProfile(source)
                .versionNumber(1).extractionMethod("CSS_SELECTOR")
                .ruleDefinition("{\"title\":\"h1\"}").isActive(true).build();
    }

    @Test
    void startRunSuccessfully() {
        CrawlSourceProfile source = buildSource(1L);
        CrawlRuleVersion rule = buildRuleVersion(10L, source);
        CrawlRunRequest request = new CrawlRunRequest(1L, 10L);

        CrawlRun savedRun = CrawlRun.builder().id(100L).sourceProfile(source)
                .ruleVersion(rule).status("PENDING").build();

        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(crawlRunRepository.save(any(CrawlRun.class))).thenReturn(savedRun);
        doNothing().when(crawlerEngine).executeCrawl(any(), any(), any());

        CrawlRun result = crawlRunService.startRun(request, 5L);

        assertEquals(100L, result.getId());
        assertEquals("PENDING", result.getStatus());
        verify(crawlerEngine).executeCrawl(savedRun, source, rule);
        verify(auditLogService).log(eq(5L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void startRunThrowsNotFoundForMissingSource() {
        when(crawlSourceProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> crawlRunService.startRun(new CrawlRunRequest(999L, 10L), 5L));
        verify(crawlRunRepository, never()).save(any());
    }

    @Test
    void startRunThrowsNotFoundForMissingRuleVersion() {
        CrawlSourceProfile source = buildSource(1L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(source));
        when(crawlRuleVersionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> crawlRunService.startRun(new CrawlRunRequest(1L, 999L), 5L));
        verify(crawlRunRepository, never()).save(any());
    }

    @Test
    void getRunSuccessfully() {
        CrawlRun run = CrawlRun.builder().id(100L).status("RUNNING").build();
        when(crawlRunRepository.findById(100L)).thenReturn(Optional.of(run));

        CrawlRun result = crawlRunService.getRun(100L);

        assertEquals(100L, result.getId());
        assertEquals("RUNNING", result.getStatus());
    }

    @Test
    void getRunThrowsNotFoundForMissingId() {
        when(crawlRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> crawlRunService.getRun(999L));
    }

    @Test
    void listRunsWithoutSourceFilter() {
        Page<CrawlRun> page = new PageImpl<>(List.of(
                CrawlRun.builder().id(1L).status("COMPLETED").build(),
                CrawlRun.builder().id(2L).status("PENDING").build()));

        when(crawlRunRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<CrawlRun> result = crawlRunService.listRuns(Pageable.unpaged(), null);

        assertEquals(2, result.getTotalElements());
        verify(crawlRunRepository).findAll(any(Pageable.class));
    }

    @Test
    void listRunsWithSourceFilter() {
        Page<CrawlRun> page = new PageImpl<>(List.of(
                CrawlRun.builder().id(1L).status("COMPLETED").build()));

        when(crawlRunRepository.findBySourceProfileId(eq(5L), any(Pageable.class))).thenReturn(page);

        Page<CrawlRun> result = crawlRunService.listRuns(Pageable.unpaged(), 5L);

        assertEquals(1, result.getTotalElements());
        verify(crawlRunRepository).findBySourceProfileId(eq(5L), any(Pageable.class));
    }

    @Test
    void cancelPendingRunSuccessfully() {
        CrawlRun run = CrawlRun.builder().id(100L).status("PENDING").build();
        when(crawlRunRepository.findById(100L)).thenReturn(Optional.of(run));
        when(crawlRunRepository.save(run)).thenReturn(run);

        CrawlRun result = crawlRunService.cancelRun(100L, 5L);

        assertEquals("CANCELLED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        verify(auditLogService).log(eq(5L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelAlreadyCompletedRunThrowsIllegalState() {
        CrawlRun run = CrawlRun.builder().id(100L).status("COMPLETED").build();
        when(crawlRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> crawlRunService.cancelRun(100L, 5L));
        verify(crawlRunRepository, never()).save(any());
    }

    @Test
    void cancelAlreadyCancelledRunThrowsIllegalState() {
        CrawlRun run = CrawlRun.builder().id(100L).status("CANCELLED").build();
        when(crawlRunRepository.findById(100L)).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> crawlRunService.cancelRun(100L, 5L));
    }
}
