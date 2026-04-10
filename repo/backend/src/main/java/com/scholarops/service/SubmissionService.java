package com.scholarops.service;

import com.scholarops.exception.*;
import com.scholarops.model.dto.AutosaveRequest;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import com.scholarops.util.FingerprintUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final QuizPaperRepository quizPaperRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AutoGradingService autoGradingService;
    private final PlagiarismService plagiarismService;
    private final AuditLogService auditLogService;
    private final GradingStateRepository gradingStateRepository;

    public SubmissionService(SubmissionRepository submissionRepository, SubmissionAnswerRepository answerRepository,
            QuizPaperRepository quizPaperRepository, QuestionRepository questionRepository,
            UserRepository userRepository, AutoGradingService autoGradingService,
            PlagiarismService plagiarismService, AuditLogService auditLogService,
            GradingStateRepository gradingStateRepository) {
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.quizPaperRepository = quizPaperRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.autoGradingService = autoGradingService;
        this.plagiarismService = plagiarismService;
        this.auditLogService = auditLogService;
        this.gradingStateRepository = gradingStateRepository;
    }

    @Transactional
    public Submission startSubmission(Long quizPaperId, Long studentId) {
        QuizPaper quiz = quizPaperRepository.findById(quizPaperId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        if (!quiz.getIsPublished()) throw new ForbiddenException("Quiz is not published");

        LocalDateTime now = LocalDateTime.now();
        if (quiz.getReleaseStart() != null && now.isBefore(quiz.getReleaseStart()))
            throw new ForbiddenException("Quiz is not yet available");
        if (quiz.getReleaseEnd() != null && now.isAfter(quiz.getReleaseEnd()))
            throw new ForbiddenException("Quiz release window has ended");

        long attemptCount = submissionRepository.countByQuizPaperIdAndStudentId(quizPaperId, studentId);
        if (attemptCount >= quiz.getMaxAttempts())
            throw new ConflictException("Maximum attempts (" + quiz.getMaxAttempts() + ") reached");

        User student = userRepository.findById(studentId).orElseThrow();
        Submission submission = Submission.builder()
                .quizPaper(quiz).student(student)
                .attemptNumber((int) attemptCount + 1)
                .status("IN_PROGRESS")
                .timeRemainingSeconds(quiz.getTimeLimitMinutes() != null ? quiz.getTimeLimitMinutes() * 60 : null)
                .maxScore(quiz.getTotalPoints())
                .build();
        submission = submissionRepository.save(submission);

        auditLogService.log(studentId, AuditAction.SUBMISSION_START, "Submission", submission.getId(),
                "Started quiz attempt " + submission.getAttemptNumber(), null, null);
        return submission;
    }

    @Transactional
    public void autosave(Long submissionId, AutosaveRequest request, Long studentId) {
        Submission submission = getOwnSubmission(submissionId, studentId);
        if (!"IN_PROGRESS".equals(submission.getStatus())) return;

        if (request.getAnswers() != null) {
            for (AutosaveRequest.AnswerEntry entry : request.getAnswers()) {
                SubmissionAnswer answer = answerRepository
                        .findBySubmissionIdAndQuestionId(submission.getId(), entry.getQuestionId())
                        .orElseGet(() -> {
                            Question q = questionRepository.findById(entry.getQuestionId()).orElseThrow();
                            return SubmissionAnswer.builder().submission(submission).question(q).build();
                        });
                answer.setAnswerText(entry.getAnswerText());
                answer.setSelectedOption(entry.getSelectedOption());
                answerRepository.save(answer);
            }
        }
        submission.setTimeRemainingSeconds(request.getTimeRemainingSeconds());
        submission.setAutoSavedAt(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    @Transactional
    public Submission submitSubmission(Long submissionId, Long studentId) {
        Submission submission = getOwnSubmission(submissionId, studentId);
        if (!"IN_PROGRESS".equals(submission.getStatus()))
            throw new ConflictException("Submission already submitted");

        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());

        String allAnswers = submission.getAnswers().stream()
                .map(a -> a.getAnswerText() != null ? a.getAnswerText() : "")
                .collect(Collectors.joining(" "));
        submission.setFingerprintHash(FingerprintUtil.hashText(allAnswers));

        submission = submissionRepository.save(submission);
        autoGradingService.gradeObjectiveAnswers(submission);
        plagiarismService.checkSubmission(submission);

        auditLogService.log(studentId, AuditAction.SUBMISSION_SUBMIT, "Submission", submissionId,
                "Submitted quiz", null, null);
        return submission;
    }

    public Submission getSubmission(Long id, Long userId, boolean isInstructor, boolean isTeachingAssistant) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + id));
        if (submission.getStudent().getId().equals(userId)) {
            return submission;
        }

        if (isInstructor) {
            Long quizOwnerId = submission.getQuizPaper().getCreatedBy().getId();
            if (quizOwnerId != null && quizOwnerId.equals(userId)) {
                return submission;
            }
        }

        if (isTeachingAssistant && gradingStateRepository.existsBySubmissionIdAndAssignedToId(id, userId)) {
            return submission;
        }

        throw new ForbiddenException("Not authorized to access this submission");
    }

    public Map<String, Object> getFeedback(Long submissionId, Long studentId) {
        Submission submission = getOwnSubmission(submissionId, studentId);
        if (!"GRADED".equals(submission.getStatus()) && !"RELEASED".equals(submission.getStatus())) {
            throw new ForbiddenException("Feedback is not yet available for this submission");
        }
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("submissionId", submissionId);
        feedback.put("status", submission.getStatus());
        feedback.put("totalScore", submission.getTotalScore());
        feedback.put("maxScore", submission.getMaxScore());
        List<Map<String, Object>> sanitizedAnswers = new ArrayList<>();
        for (SubmissionAnswer answer : submission.getAnswers()) {
            Map<String, Object> answerMap = new HashMap<>();
            answerMap.put("questionId", answer.getQuestion().getId());
            answerMap.put("answerText", answer.getAnswerText());
            answerMap.put("selectedOption", answer.getSelectedOption());
            answerMap.put("score", answer.getScore());
            answerMap.put("isCorrect", answer.getIsCorrect());
            sanitizedAnswers.add(answerMap);
        }
        feedback.put("answers", sanitizedAnswers);
        return feedback;
    }

    private Submission getOwnSubmission(Long submissionId, Long studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
        if (!submission.getStudent().getId().equals(studentId))
            throw new ForbiddenException("Not authorized to access this submission");
        return submission;
    }
}
