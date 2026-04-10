package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.model.entity.SubmissionAnswer;
import com.scholarops.model.entity.User;
import com.scholarops.repository.GradingStateRepository;
import com.scholarops.repository.RubricScoreRepository;
import com.scholarops.repository.SubmissionAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingAuthorizationTest {

    @Mock private GradingStateRepository gradingStateRepository;
    @Mock private RubricScoreRepository rubricScoreRepository;
    @Mock private SubmissionAnswerRepository answerRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private GradingWorkflowService gradingWorkflowService;

    @Test
    void graderCanAccessGradingStateAssignedToThem() {
        User grader = User.builder().id(50L).username("grader").build();
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").assignedTo(grader).build();

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));

        GradingState result = gradingWorkflowService.getGradingStateForGrader(1L, 50L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(50L, result.getAssignedTo().getId());
    }

    @Test
    void graderCannotAccessGradingStateAssignedToSomeoneElse() {
        User assignedGrader = User.builder().id(50L).username("grader1").build();
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").assignedTo(assignedGrader).build();

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));

        assertThrows(ForbiddenException.class,
                () -> gradingWorkflowService.getGradingStateForGrader(1L, 99L));
    }

    @Test
    void gradeItemThrowsForbiddenWhenGraderIsNotAssigned() {
        User assignedGrader = User.builder().id(50L).username("grader1").build();
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").assignedTo(assignedGrader).build();

        GradingRequest request = new GradingRequest();
        request.setScore(8.0);
        request.setFeedback("Good work");

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));

        assertThrows(ForbiddenException.class,
                () -> gradingWorkflowService.gradeItem(1L, request, 99L));
    }

    @Test
    void addRubricScoresThrowsForbiddenWhenGraderIsNotAssigned() {
        User assignedGrader = User.builder().id(50L).username("grader1").build();
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").assignedTo(assignedGrader).build();

        RubricScoreRequest score = new RubricScoreRequest();
        score.setCriterionName("Clarity");
        score.setMaxScore(5.0);
        score.setAwardedScore(4.0);

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));

        assertThrows(ForbiddenException.class,
                () -> gradingWorkflowService.addRubricScores(1L, List.of(score), 99L));
    }
}
