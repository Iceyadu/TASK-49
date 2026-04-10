package com.scholarops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.entity.*;
import com.scholarops.repository.MediaMetadataRepository;
import com.scholarops.repository.StandardizedContentRecordRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentStandardizationServiceTest {

    @Mock private StandardizedContentRecordRepository contentRepository;
    @Mock private MediaMetadataRepository mediaMetadataRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ContentStandardizationService service;

    @Test
    void testStandardizeWithTimestamp() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("title", "Test Article");
        rawData.put("timestamp", "2024-01-15T10:30:00Z");
        rawData.put("bodyText", "The quick brown fox jumps over the lazy dog");

        CrawlRun crawlRun = CrawlRun.builder().id(1L).build();
        CrawlSourceProfile source = CrawlSourceProfile.builder().id(1L).build();

        when(contentRepository.save(any())).thenAnswer(inv -> {
            StandardizedContentRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        StandardizedContentRecord result = service.standardize(rawData, "America/New_York",
                crawlRun, source);

        assertNotNull(result);
        assertEquals("Test Article", result.getTitle());
        // TimestampNormalizer.normalize is now called statically
        assertNotNull(result.getStandardizedTimestamp());
    }

    @Test
    void testStandardizeWithAddress() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("title", "Location Item");
        rawData.put("location", "123 Main St, Springfield, IL 62701");

        CrawlRun crawlRun = CrawlRun.builder().id(1L).build();
        CrawlSourceProfile source = CrawlSourceProfile.builder().id(1L).build();

        when(contentRepository.save(any())).thenAnswer(inv -> {
            StandardizedContentRecord r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        StandardizedContentRecord result = service.standardize(rawData, null, crawlRun, source);

        assertNotNull(result);
        assertEquals("123 Main St, Springfield, IL 62701", result.getOriginalLocation());
        // AddressNormalizer.normalize is now called statically
        assertNotNull(result.getNormalizedAddress());
    }

    @Test
    void testStandardizeWithLanguageDetection() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("title", "Un articulo en espanol");
        rawData.put("bodyText", "La casa de los suenos es muy bonita para todos");

        CrawlRun crawlRun = CrawlRun.builder().id(1L).build();
        CrawlSourceProfile source = CrawlSourceProfile.builder().id(1L).build();

        when(contentRepository.save(any())).thenAnswer(inv -> {
            StandardizedContentRecord r = inv.getArgument(0);
            r.setId(3L);
            return r;
        });

        StandardizedContentRecord result = service.standardize(rawData, null, crawlRun, source);

        // LanguageDetector.detect is now called statically
        assertEquals("es", result.getDetectedLanguage());
    }

    @Test
    void testPublishContent() {
        User publisher = User.builder().id(1L).username("admin").build();
        StandardizedContentRecord record = new StandardizedContentRecord();
        record.setId(10L);
        record.setTitle("Published Article");

        when(userRepository.findById(1L)).thenReturn(Optional.of(publisher));
        when(contentRepository.findById(10L)).thenReturn(Optional.of(record));
        when(contentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.publishContent(List.of(10L), 1L);

        assertTrue(record.getIsPublished());
        assertNotNull(record.getPublishedAt());
        assertEquals(publisher, record.getPublishedBy());
        verify(auditLogService).log(eq(1L), any(), any(), eq(10L), any(), any(), any());
    }
}
