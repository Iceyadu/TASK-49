package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false)
    private CrawlSourceProfile sourceProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_version_id", nullable = false)
    private CrawlRuleVersion ruleVersion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_pages")
    @Builder.Default
    private Integer totalPages = 0;

    @Column(name = "pages_crawled")
    @Builder.Default
    private Integer pagesCrawled = 0;

    @Column(name = "pages_failed")
    @Builder.Default
    private Integer pagesFailed = 0;

    @Column(name = "items_extracted")
    @Builder.Default
    private Integer itemsExtracted = 0;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
