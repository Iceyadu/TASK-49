package com.scholarops.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_source_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlSourceProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "base_url", nullable = false, length = 2000)
    private String baseUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rate_limit_per_minute", nullable = false)
    @Builder.Default
    private Integer rateLimitPerMinute = 30;

    @Column(name = "requires_auth", nullable = false)
    @Builder.Default
    private Boolean requiresAuth = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "sourceProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private EncryptedSourceCredential credential;

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
