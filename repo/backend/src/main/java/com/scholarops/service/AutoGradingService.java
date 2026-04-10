package com.scholarops.service;

import com.scholarops.model.entity.*;
import com.scholarops.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AutoGradingService {
    private final SubmissionAnswerRepository answerRepository;
    private final WrongAnswerHistoryRepository wrongAnswerRepository;
    private final GradingWorkflowService gradingWorkflowService;

    public AutoGradingService(SubmissionAnswerRepository answerRepository,
            WrongAnswerHistoryRepository wrongAnswerRepository,
            GradingWorkflowService gradingWorkflowService) {
        this.answerRepository = answerRepository;
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.gradingWorkflowService = gradingWorkflowService;
    }

    @Transactional
    public void gradeObjectiveAnswers(Submission submission) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (SubmissionAnswer answer : submission.getAnswers()) {
            Question question = answer.getQuestion();
            if (question.isObjective()) {
                boolean correct = checkAnswer(answer, question);
                answer.setIsCorrect(correct);
                answer.setAutoGraded(true);
                answer.setGradedAt(LocalDateTime.now());
                answer.setScore(correct ? question.getPoints() : BigDecimal.ZERO);
                answerRepository.save(answer);

                if (correct) {
                    totalScore = totalScore.add(question.getPoints());
                } else {
                    WrongAnswerHistory wrong = WrongAnswerHistory.builder()
                            .student(submission.getStudent())
                            .question(question)
                            .submission(submission)
                            .studentAnswer(answer.getAnswerText() != null ? answer.getAnswerText() : answer.getSelectedOption())
                            .correctAnswer(question.getCorrectAnswer())
                            .explanation(question.getExplanation())
                            .build();
                    wrongAnswerRepository.save(wrong);
                }
            } else {
                gradingWorkflowService.routeToQueue(answer);
            }
        }

        submission.setTotalScore(totalScore);
        submission.setStatus("AUTO_GRADED");
    }

    private boolean checkAnswer(SubmissionAnswer answer, Question question) {
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null) return false;
        String studentAnswer = answer.getSelectedOption() != null ? answer.getSelectedOption() : answer.getAnswerText();
        if (studentAnswer == null) return false;
        return correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
    }
}
