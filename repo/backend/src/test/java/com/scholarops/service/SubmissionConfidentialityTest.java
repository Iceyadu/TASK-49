package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.model.entity.*;
import com.scholarops.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionConfidentialityTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubmissionAnswerRepository answerRepository;
    @Mock private QuizPaperRepository quizPaperRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AutoGradingService autoGradingService;
    @Mock private PlagiarismService plagiarismService;
    @Mock private AuditLogService auditLogService;
    @Mock private GradingStateRepository gradingStateRepository;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void getFeedbackThrowsExceptionBeforeGradingIsComplete() {
        User student = User.builder().id(10L).build();
        Submission submission = Submission.builder()
                .id(100L).student(student).status("SUBMITTED").build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        assertThrows(ForbiddenException.class,
                () -> submissionService.getFeedback(100L, 10L));
    }

    @Test
    void getFeedbackReturnsSanitizedAnswerDataAfterGrading() {
        User student = User.builder().id(10L).build();
        Question question = Question.builder().id(1L).questionText("What is 2+2?")
                .correctAnswer("4").explanation("Basic arithmetic").build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).question(question).answerText("4")
                .selectedOption("A").score(BigDecimal.TEN).isCorrect(true).build();
        Submission submission = Submission.builder()
                .id(100L).student(student).status("GRADED")
                .totalScore(BigDecimal.TEN).maxScore(BigDecimal.TEN)
                .answers(new ArrayList<>(List.of(answer))).build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        Map<String, Object> feedback = submissionService.getFeedback(100L, 10L);

        assertNotNull(feedback);
        assertEquals(100L, feedback.get("submissionId"));
        assertEquals("GRADED", feedback.get("status"));
        assertEquals(BigDecimal.TEN, feedback.get("totalScore"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) feedback.get("answers");
        assertEquals(1, answers.size());

        Map<String, Object> answerMap = answers.get(0);
        assertEquals(1L, answerMap.get("questionId"));
        assertEquals("4", answerMap.get("answerText"));
        assertEquals("A", answerMap.get("selectedOption"));
        assertEquals(BigDecimal.TEN, answerMap.get("score"));
        assertEquals(true, answerMap.get("isCorrect"));

        // Verify that correctAnswer and explanation are NOT exposed in the feedback
        assertFalse(answerMap.containsKey("correctAnswer"));
        assertFalse(answerMap.containsKey("explanation"));
    }

    @Test
    void getSubmissionDeniesUnauthorizedInstructor() {
        User student = User.builder().id(10L).build();
        User quizOwner = User.builder().id(20L).build();
        QuizPaper quizPaper = QuizPaper.builder().id(1L).createdBy(quizOwner).build();
        Submission submission = Submission.builder()
                .id(100L).student(student).quizPaper(quizPaper).build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        // Instructor with userId=30 did not create the quiz (owner is 20)
        assertThrows(ForbiddenException.class,
                () -> submissionService.getSubmission(100L, 30L, true, false));
    }
}
