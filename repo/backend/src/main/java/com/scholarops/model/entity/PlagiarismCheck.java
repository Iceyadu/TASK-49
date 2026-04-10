package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plagiarism_checks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlagiarismCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "max_similarity_score", precision = 5, scale = 4)
    private BigDecimal maxSimilarityScore;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @OneToMany(mappedBy = "plagiarismCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlagiarismMatch> matches = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
