package com.scholarops.service;

import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.model.entity.SubmissionAnswer;
import com.scholarops.repository.GradingStateRepository;
import com.scholarops.repository.RubricScoreRepository;
import com.scholarops.repository.SubmissionAnswerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingWorkflowServiceTest {

    @Mock private GradingStateRepository gradingStateRepository;
    @Mock private RubricScoreRepository rubricScoreRepository;
    @Mock private SubmissionAnswerRepository answerRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private GradingWorkflowService gradingWorkflowService;

    @Test
    void testRouteToQueue() {
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        when(gradingStateRepository.save(any(GradingState.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        gradingWorkflowService.routeToQueue(answer);

        verify(gradingStateRepository).save(argThat(state ->
                "PENDING".equals(state.getStatus()) &&
                state.getSubmissionAnswer().equals(answer)
        ));
    }

    @Test
    void testGradeItem() {
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").build();

        GradingRequest request = new GradingRequest();
        request.setScore(8.0);
        request.setFeedback("Good work");

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(gradingStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GradingState result = gradingWorkflowService.gradeItem(1L, request, 99L);

        assertEquals("GRADED", result.getStatus());
        assertEquals(0, BigDecimal.valueOf(8).compareTo(result.getScore()));
        assertEquals("Good work", result.getFeedback());
        assertNotNull(result.getGradedAt());
        verify(answerRepository).save(answer);
    }

    @Test
    void testAddRubricScoresMandatory() {
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").build();

        RubricScoreRequest score1 = new RubricScoreRequest();
        score1.setCriterionName("Clarity");
        score1.setMaxScore(5.0);
        score1.setAwardedScore(4.0);

        RubricScoreRequest score2 = new RubricScoreRequest();
        score2.setCriterionName("Depth");
        score2.setMaxScore(5.0);
        score2.setAwardedScore(3.0);

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));
        when(gradingStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rubricScoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GradingState result = gradingWorkflowService.addRubricScores(
                1L, List.of(score1, score2), 99L);

        assertEquals("GRADED", result.getStatus());
        assertEquals(0, BigDecimal.valueOf(7).compareTo(result.getScore()));
        verify(rubricScoreRepository, times(2)).save(any());
    }

    @Test
    void testAddRubricScoresEmpty() {
        SubmissionAnswer answer = SubmissionAnswer.builder().id(1L).build();
        GradingState state = GradingState.builder()
                .id(1L).submissionAnswer(answer).status("PENDING").build();

        when(gradingStateRepository.findById(1L)).thenReturn(Optional.of(state));

        assertThrows(IllegalArgumentException.class,
                () -> gradingWorkflowService.addRubricScores(1L, List.of(), 99L));
    }
}
