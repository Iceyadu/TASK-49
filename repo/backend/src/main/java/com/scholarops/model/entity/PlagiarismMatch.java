package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plagiarism_matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlagiarismMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plagiarism_check_id", nullable = false)
    private PlagiarismCheck plagiarismCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_submission_id")
    private Submission matchedSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_content_id")
    private StandardizedContentRecord matchedContent;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "matched_text_excerpt", columnDefinition = "TEXT")
    private String matchedTextExcerpt;

    @Column(name = "source_text_excerpt", columnDefinition = "TEXT")
    private String sourceTextExcerpt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
