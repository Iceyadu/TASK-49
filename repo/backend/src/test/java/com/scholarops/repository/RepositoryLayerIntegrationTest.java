package com.scholarops.repository;

import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.Schedule;
import com.scholarops.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RepositoryLayerIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private CrawlSourceProfileRepository crawlSourceProfileRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void userRepositoryFindsSeededAdminAndSearchesByKeyword() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        assertTrue(userRepository.existsByUsername("admin"));
        assertTrue(userRepository.existsByEmail("admin@scholarops.local"));
        assertEquals("admin@scholarops.local", admin.getEmail());

        List<User> search = userRepository.searchByKeyword("admin");
        assertFalse(search.isEmpty());
    }

    @Test
    void scheduleRepositoryReturnsConflictingSchedulesForSameUser() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(1);

        scheduleRepository.save(Schedule.builder()
                .user(admin)
                .title("Repository Layer Test Schedule")
                .startTime(start)
                .endTime(end)
                .isRecurring(false)
                .build());

        List<Schedule> conflicts = scheduleRepository.findConflicting(
                admin.getId(),
                start.plusMinutes(15),
                end.minusMinutes(15));

        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(s -> "Repository Layer Test Schedule".equals(s.getTitle())));
    }

    @Test
    void crawlSourceProfileRepositoryFiltersByCreatorAndEnabledFlag() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        jdbcTemplate.update(
                "INSERT INTO crawl_source_profiles " +
                        "(name, base_url, description, rate_limit_per_minute, requires_auth, enabled, created_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                "repo-layer-source",
                "https://example.org",
                "Repository layer coverage",
                25,
                false,
                true,
                admin.getId());

        List<CrawlSourceProfile> byCreator = crawlSourceProfileRepository.findByCreatedById(admin.getId());
        assertTrue(byCreator.stream().anyMatch(p -> "repo-layer-source".equals(p.getName())));

        List<CrawlSourceProfile> enabled = crawlSourceProfileRepository.findByEnabled(true);
        assertTrue(enabled.stream().anyMatch(p -> "repo-layer-source".equals(p.getName())));
    }
}
