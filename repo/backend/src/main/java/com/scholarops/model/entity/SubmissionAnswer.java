package com.scholarops.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "submission_answers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id", "question_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmissionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "selected_option", length = 10)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "auto_graded", nullable = false)
    @Builder.Default
    private Boolean autoGraded = false;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

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
