package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "standardized_content_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StandardizedContentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crawl_run_id")
    private CrawlRun crawlRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false)
    private CrawlSourceProfile sourceProfile;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp;

    @Column(name = "standardized_timestamp")
    private LocalDateTime standardizedTimestamp;

    @Column(name = "timezone_id", length = 50)
    private String timezoneId;

    @Column(name = "original_location", length = 500)
    private String originalLocation;

    @Column(name = "normalized_address", length = 500)
    private String normalizedAddress;

    @Column(name = "detected_language", length = 10)
    private String detectedLanguage;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "availability_start")
    private LocalDate availabilityStart;

    @Column(name = "availability_end")
    private LocalDate availabilityEnd;

    @Column(name = "popularity_score", nullable = false)
    @Builder.Default
    private Integer popularityScore = 0;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
