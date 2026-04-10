package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rubric_scores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RubricScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_state_id", nullable = false)
    private GradingState gradingState;

    @Column(name = "criterion_name", nullable = false, length = 200)
    private String criterionName;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "awarded_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal awardedScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
