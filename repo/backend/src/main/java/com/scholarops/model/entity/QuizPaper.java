package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_papers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizPaper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_bank_id", nullable = false)
    private QuestionBank questionBank;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "total_points", nullable = false, precision = 7, scale = 2)
    private BigDecimal totalPoints;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(name = "release_start")
    private LocalDateTime releaseStart;

    @Column(name = "release_end")
    private LocalDateTime releaseEnd;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    private Boolean shuffleQuestions = false;

    @Column(name = "show_immediate_feedback", nullable = false)
    @Builder.Default
    private Boolean showImmediateFeedback = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "quiz_questions",
        joinColumns = @JoinColumn(name = "quiz_paper_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id"))
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quizPaper", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizRule> rules = new ArrayList<>();

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
