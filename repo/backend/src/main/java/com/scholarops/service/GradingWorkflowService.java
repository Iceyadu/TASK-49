package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GradingWorkflowService {
    private final GradingStateRepository gradingStateRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final AuditLogService auditLogService;

    public GradingWorkflowService(GradingStateRepository gradingStateRepository,
            RubricScoreRepository rubricScoreRepository, SubmissionAnswerRepository answerRepository,
            AuditLogService auditLogService) {
        this.gradingStateRepository = gradingStateRepository;
        this.rubricScoreRepository = rubricScoreRepository;
        this.answerRepository = answerRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void routeToQueue(SubmissionAnswer answer) {
        GradingState state = GradingState.builder()
                .submissionAnswer(answer).status("PENDING").build();
        gradingStateRepository.save(state);
    }

    public Page<GradingState> getGradingQueue(Pageable pageable, String status) {
        return gradingStateRepository.findByStatus(status, pageable);
    }

    public GradingState getGradingState(Long id) {
        return gradingStateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grading state not found: " + id));
    }

    public GradingState getGradingStateForGrader(Long id, Long graderId) {
        GradingState state = getGradingState(id);
        if (state.getAssignedTo() != null && !state.getAssignedTo().getId().equals(graderId)) {
            throw new ForbiddenException("Not authorized to access this grading state");
        }
        return state;
    }

    @Transactional
    public GradingState gradeItem(Long gradingStateId, GradingRequest request, Long graderId) {
        GradingState state = getGradingState(gradingStateId);
        if (state.getAssignedTo() != null && !state.getAssignedTo().getId().equals(graderId)) {
            throw new ForbiddenException("Not authorized to grade this submission");
        }
        state.setScore(request.getScore() != null ? BigDecimal.valueOf(request.getScore()) : null);
        state.setFeedback(request.getFeedback());
        state.setStatus("GRADED");
        state.setGradedAt(LocalDateTime.now());

        SubmissionAnswer answer = state.getSubmissionAnswer();
        answer.setScore(request.getScore() != null ? BigDecimal.valueOf(request.getScore()) : null);
        answer.setGradedAt(LocalDateTime.now());
        answerRepository.save(answer);

        auditLogService.log(graderId, AuditAction.GRADING_SUBMIT, "GradingState", gradingStateId,
                "Graded submission answer", null, null);
        return gradingStateRepository.save(state);
    }

    @Transactional
    public GradingState addRubricScores(Long gradingStateId, List<RubricScoreRequest> rubricScores, Long graderId) {
        GradingState state = getGradingState(gradingStateId);
        if (state.getAssignedTo() != null && !state.getAssignedTo().getId().equals(graderId)) {
            throw new ForbiddenException("Not authorized to grade this submission");
        }
        if (rubricScores == null || rubricScores.isEmpty()) {
            throw new IllegalArgumentException("Rubric scores are mandatory for subjective grading");
        }

        BigDecimal totalAwarded = BigDecimal.ZERO;
        for (RubricScoreRequest scoreReq : rubricScores) {
            RubricScore rubric = RubricScore.builder()
                    .gradingState(state)
                    .criterionName(scoreReq.getCriterionName())
                    .maxScore(BigDecimal.valueOf(scoreReq.getMaxScore()))
                    .awardedScore(BigDecimal.valueOf(scoreReq.getAwardedScore()))
                    .comment(scoreReq.getComment())
                    .build();
            rubricScoreRepository.save(rubric);
            totalAwarded = totalAwarded.add(BigDecimal.valueOf(scoreReq.getAwardedScore()));
        }

        state.setScore(totalAwarded);
        state.setStatus("GRADED");
        state.setGradedAt(LocalDateTime.now());

        SubmissionAnswer answer = state.getSubmissionAnswer();
        answer.setScore(totalAwarded);
        answer.setGradedAt(LocalDateTime.now());
        answerRepository.save(answer);

        auditLogService.log(graderId, AuditAction.GRADING_SUBMIT, "GradingState", gradingStateId,
                "Added rubric scores", null, null);
        return gradingStateRepository.save(state);
    }
}
