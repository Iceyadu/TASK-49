package com.scholarops.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;

    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "JSON")
    private String options;

    @JsonIgnore
    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal points = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "question_knowledge_tags",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<KnowledgeTag> knowledgeTags = new HashSet<>();

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

    public boolean isObjective() {
        return "MULTIPLE_CHOICE".equals(questionType)
            || "TRUE_FALSE".equals(questionType)
            || "FILL_IN_BLANK".equals(questionType);
    }
}
