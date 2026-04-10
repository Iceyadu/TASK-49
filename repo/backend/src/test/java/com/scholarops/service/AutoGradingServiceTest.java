package com.scholarops.service;

import com.scholarops.model.entity.*;
import com.scholarops.repository.SubmissionAnswerRepository;
import com.scholarops.repository.WrongAnswerHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoGradingServiceTest {

    @Mock private SubmissionAnswerRepository answerRepository;
    @Mock private WrongAnswerHistoryRepository wrongAnswerRepository;
    @Mock private GradingWorkflowService gradingWorkflowService;

    @InjectMocks
    private AutoGradingService autoGradingService;

    @Test
    void testGradeObjectiveCorrect() {
        Question question = Question.builder()
                .id(1L).questionType("MULTIPLE_CHOICE")
                .correctAnswer("B").points(BigDecimal.valueOf(5)).build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).question(question).selectedOption("B").build();
        User student = User.builder().id(10L).build();
        Submission submission = Submission.builder()
                .id(100L).student(student)
                .answers(new ArrayList<>(List.of(answer))).build();

        autoGradingService.gradeObjectiveAnswers(submission);

        assertTrue(answer.getIsCorrect());
        assertTrue(answer.getAutoGraded());
        assertEquals(BigDecimal.valueOf(5), answer.getScore());
        assertEquals(BigDecimal.valueOf(5), submission.getTotalScore());
        verify(wrongAnswerRepository, never()).save(any());
    }

    @Test
    void testGradeObjectiveIncorrect() {
        Question question = Question.builder()
                .id(1L).questionType("MULTIPLE_CHOICE")
                .correctAnswer("B").points(BigDecimal.valueOf(5))
                .explanation("B is correct because...").build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).question(question).selectedOption("C").build();
        User student = User.builder().id(10L).build();
        Submission submission = Submission.builder()
                .id(100L).student(student)
                .answers(new ArrayList<>(List.of(answer))).build();

        autoGradingService.gradeObjectiveAnswers(submission);

        assertFalse(answer.getIsCorrect());
        assertEquals(BigDecimal.ZERO, answer.getScore());
        assertEquals(BigDecimal.ZERO, submission.getTotalScore());
        verify(wrongAnswerRepository).save(any(WrongAnswerHistory.class));
    }

    @Test
    void testSubjectiveRoutedToQueue() {
        Question question = Question.builder()
                .id(1L).questionType("ESSAY")
                .points(BigDecimal.valueOf(10)).build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).question(question).answerText("My essay response").build();
        User student = User.builder().id(10L).build();
        Submission submission = Submission.builder()
                .id(100L).student(student)
                .answers(new ArrayList<>(List.of(answer))).build();

        autoGradingService.gradeObjectiveAnswers(submission);

        verify(gradingWorkflowService).routeToQueue(answer);
        verify(answerRepository, never()).save(answer);
    }

    @Test
    void testWrongAnswerHistoryCreated() {
        Question question = Question.builder()
                .id(1L).questionType("TRUE_FALSE")
                .correctAnswer("True").points(BigDecimal.ONE)
                .explanation("The statement is true").build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).question(question).selectedOption("False").build();
        User student = User.builder().id(10L).build();
        Submission submission = Submission.builder()
                .id(100L).student(student)
                .answers(new ArrayList<>(List.of(answer))).build();

        autoGradingService.gradeObjectiveAnswers(submission);

        verify(wrongAnswerRepository).save(argThat(wrong ->
                "False".equals(wrong.getStudentAnswer()) &&
                "True".equals(wrong.getCorrectAnswer()) &&
                "The statement is true".equals(wrong.getExplanation())
        ));
    }
}
