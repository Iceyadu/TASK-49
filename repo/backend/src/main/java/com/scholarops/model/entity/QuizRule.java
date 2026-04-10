package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_paper_id", nullable = false)
    private QuizPaper quizPaper;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(name = "min_count")
    private Integer minCount;

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private KnowledgeTag tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
