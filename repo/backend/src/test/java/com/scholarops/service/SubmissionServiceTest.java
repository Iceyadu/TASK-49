package com.scholarops.service;

import com.scholarops.exception.ConflictException;
import com.scholarops.exception.ForbiddenException;
import com.scholarops.model.dto.AutosaveRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

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

    private QuizPaper createPublishedQuiz() {
        return QuizPaper.builder()
                .id(1L).title("Test Quiz").isPublished(true)
                .maxAttempts(3).timeLimitMinutes(60)
                .totalPoints(BigDecimal.TEN).build();
    }

    @Test
    void testStartSubmission() {
        QuizPaper quiz = createPublishedQuiz();
        User student = User.builder().id(10L).username("student").build();

        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(submissionRepository.countByQuizPaperIdAndStudentId(1L, 10L)).thenReturn(0L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        Submission result = submissionService.startSubmission(1L, 10L);

        assertNotNull(result);
        assertEquals(1, result.getAttemptNumber());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(3600, result.getTimeRemainingSeconds());
    }

    @Test
    void testStartSubmissionMaxAttemptsReached() {
        QuizPaper quiz = createPublishedQuiz();

        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(submissionRepository.countByQuizPaperIdAndStudentId(1L, 10L)).thenReturn(3L);

        assertThrows(ConflictException.class,
                () -> submissionService.startSubmission(1L, 10L));
    }

    @Test
    void testStartSubmissionNotPublished() {
        QuizPaper quiz = QuizPaper.builder().id(1L).isPublished(false).build();

        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));

        assertThrows(ForbiddenException.class,
                () -> submissionService.startSubmission(1L, 10L));
    }

    @Test
    void testAutosave() {
        User student = User.builder().id(10L).username("student").build();
        Submission submission = Submission.builder()
                .id(100L).student(student).status("IN_PROGRESS").build();
        Question question = Question.builder().id(1L).build();

        AutosaveRequest request = new AutosaveRequest();
        AutosaveRequest.AnswerEntry entry = new AutosaveRequest.AnswerEntry();
        entry.setQuestionId(1L);
        entry.setAnswerText("My answer");
        request.setAnswers(List.of(entry));
        request.setTimeRemainingSeconds(1800);

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(answerRepository.findBySubmissionIdAndQuestionId(100L, 1L))
                .thenReturn(Optional.empty());
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        submissionService.autosave(100L, request, 10L);

        assertEquals(1800, submission.getTimeRemainingSeconds());
        verify(answerRepository).save(any(SubmissionAnswer.class));
    }

    @Test
    void testSubmit() {
        User student = User.builder().id(10L).username("student").build();
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(1L).answerText("My final answer").build();
        Submission submission = Submission.builder()
                .id(100L).student(student).status("IN_PROGRESS")
                .answers(new ArrayList<>(List.of(answer))).build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Submission result = submissionService.submitSubmission(100L, 10L);

        assertEquals("SUBMITTED", result.getStatus());
        assertNotNull(result.getSubmittedAt());
        assertNotNull(result.getFingerprintHash());
        verify(autoGradingService).gradeObjectiveAnswers(result);
        verify(plagiarismService).checkSubmission(result);
    }

    @Test
    void getSubmissionRejectsInstructorWithoutOwnership() {
        User student = User.builder().id(10L).build();
        User quizOwner = User.builder().id(20L).build();
        QuizPaper quizPaper = QuizPaper.builder().id(1L).createdBy(quizOwner).build();
        Submission submission = Submission.builder().id(100L).student(student).quizPaper(quizPaper).build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));

        assertThrows(ForbiddenException.class,
                () -> submissionService.getSubmission(100L, 30L, true, false));
    }

    @Test
    void getSubmissionRejectsUnassignedTeachingAssistant() {
        User student = User.builder().id(10L).build();
        User quizOwner = User.builder().id(20L).build();
        QuizPaper quizPaper = QuizPaper.builder().id(1L).createdBy(quizOwner).build();
        Submission submission = Submission.builder().id(100L).student(student).quizPaper(quizPaper).build();

        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submission));
        when(gradingStateRepository.existsBySubmissionIdAndAssignedToId(100L, 40L)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> submissionService.getSubmission(100L, 40L, false, true));
    }
}
