package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_rule_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source_profile_id", "version_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrawlRuleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false)
    private CrawlSourceProfile sourceProfile;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "extraction_method", nullable = false, length = 20)
    private String extractionMethod;

    @Column(name = "rule_definition", nullable = false, columnDefinition = "JSON")
    private String ruleDefinition;

    @Column(name = "field_mappings", nullable = false, columnDefinition = "JSON")
    private String fieldMappings;

    @Column(name = "type_validations", columnDefinition = "JSON")
    private String typeValidations;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
