package com.scholarops.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_change_journal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleChangeJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @Column(name = "previous_state", columnDefinition = "JSON")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "JSON")
    private String newState;

    @Column(name = "is_undone", nullable = false)
    @Builder.Default
    private Boolean isUndone = false;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
