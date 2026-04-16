package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.CrawlSourceRequest;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.User;
import com.scholarops.repository.CrawlSourceProfileRepository;
import com.scholarops.repository.EncryptedSourceCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlSourceServiceTest {

    @Mock private CrawlSourceProfileRepository crawlSourceProfileRepository;
    @Mock private EncryptedSourceCredentialRepository encryptedSourceCredentialRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CrawlSourceService crawlSourceService;

    private CrawlSourceRequest buildRequest(String name) {
        CrawlSourceRequest req = new CrawlSourceRequest();
        req.setName(name);
        req.setBaseUrl("https://example.org/" + name);
        req.setDescription("Test source " + name);
        req.setRateLimitPerMinute(30);
        req.setRequiresAuth(false);
        return req;
    }

    private CrawlSourceProfile buildProfile(Long id, Long ownerId) {
        User owner = User.builder().id(ownerId).username("curator" + ownerId).build();
        return CrawlSourceProfile.builder()
                .id(id)
                .name("source-" + id)
                .baseUrl("https://example.org/" + id)
                .description("Source " + id)
                .rateLimitPerMinute(30)
                .requiresAuth(false)
                .enabled(true)
                .createdBy(owner)
                .build();
    }

    @Test
    void createSourceSuccessfully() {
        CrawlSourceRequest request = buildRequest("news-crawler");
        CrawlSourceProfile saved = buildProfile(1L, 5L);

        when(crawlSourceProfileRepository.save(any(CrawlSourceProfile.class))).thenReturn(saved);

        CrawlSourceProfile result = crawlSourceService.createSource(request, 5L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(crawlSourceProfileRepository).save(any(CrawlSourceProfile.class));
        verify(auditLogService).log(eq(5L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSourceSuccessfully() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        CrawlSourceProfile result = crawlSourceService.getSource(1L, 5L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getSourceThrowsForbiddenForDifferentOwner() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> crawlSourceService.getSource(1L, 99L));
    }

    @Test
    void getSourceThrowsNotFoundForMissingId() {
        when(crawlSourceProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> crawlSourceService.getSource(999L, 5L));
    }

    @Test
    void listSourcesReturnsCuratorSources() {
        List<CrawlSourceProfile> sources = List.of(buildProfile(1L, 5L), buildProfile(2L, 5L));
        when(crawlSourceProfileRepository.findByCreatedById(5L)).thenReturn(sources);

        List<CrawlSourceProfile> result = crawlSourceService.listSources(5L);

        assertEquals(2, result.size());
        verify(crawlSourceProfileRepository).findByCreatedById(5L);
    }

    @Test
    void updateSourceSuccessfully() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        CrawlSourceRequest request = buildRequest("updated-source");

        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(crawlSourceProfileRepository.save(any(CrawlSourceProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CrawlSourceProfile result = crawlSourceService.updateSource(1L, request, 5L);

        assertEquals("updated-source", result.getName());
        verify(auditLogService).log(eq(5L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateSourceThrowsForbiddenForDifferentOwner() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class,
                () -> crawlSourceService.updateSource(1L, buildRequest("hijack"), 99L));
        verify(crawlSourceProfileRepository, never()).save(any());
    }

    @Test
    void deleteSourceSuccessfully() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        doNothing().when(crawlSourceProfileRepository).delete(profile);

        crawlSourceService.deleteSource(1L, 5L);

        verify(crawlSourceProfileRepository).delete(profile);
        verify(auditLogService).log(eq(5L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteSourceThrowsForbiddenForDifferentOwner() {
        CrawlSourceProfile profile = buildProfile(1L, 5L);
        when(crawlSourceProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThrows(ForbiddenException.class, () -> crawlSourceService.deleteSource(1L, 99L));
        verify(crawlSourceProfileRepository, never()).delete(any());
    }

    @Test
    void deleteSourceThrowsNotFoundForMissingId() {
        when(crawlSourceProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> crawlSourceService.deleteSource(999L, 5L));
    }
}
