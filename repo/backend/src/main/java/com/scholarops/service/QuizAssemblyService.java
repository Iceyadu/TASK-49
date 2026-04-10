package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.QuizAssemblyRequest;
import com.scholarops.model.dto.QuizRuleDto;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizAssemblyService {
    private final QuizPaperRepository quizPaperRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuizRuleRepository quizRuleRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public QuizAssemblyService(QuizPaperRepository quizPaperRepository, QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository, QuizRuleRepository quizRuleRepository,
            UserRepository userRepository, AuditLogService auditLogService) {
        this.quizPaperRepository = quizPaperRepository;
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.quizRuleRepository = quizRuleRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public QuizPaper assembleQuiz(QuizAssemblyRequest request, Long userId) {
        QuestionBank bank = questionBankRepository.findById(request.getQuestionBankId())
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        User user = userRepository.findById(userId).orElseThrow();

        Set<Question> selectedQuestions = new LinkedHashSet<>();
        List<Question> allBankQuestions = questionRepository.findByQuestionBankId(bank.getId());

        if (request.getRules() != null) {
            for (QuizRuleDto rule : request.getRules()) {
                if ("DIFFICULTY".equals(rule.getRuleType()) && rule.getDifficultyLevel() != null) {
                    List<Question> matching = allBankQuestions.stream()
                            .filter(q -> q.getDifficultyLevel().equals(rule.getDifficultyLevel()))
                            .filter(q -> !selectedQuestions.contains(q))
                            .collect(Collectors.toList());
                    int needed = rule.getMinCount() != null ? rule.getMinCount() : 0;
                    if (matching.size() < needed) {
                        throw new IllegalArgumentException("Not enough questions with difficulty " +
                                rule.getDifficultyLevel() + ": need " + needed + " but only " + matching.size() + " available");
                    }
                    Collections.shuffle(matching);
                    selectedQuestions.addAll(matching.subList(0, Math.min(needed, matching.size())));
                }
            }
        }

        List<Question> remaining = allBankQuestions.stream()
                .filter(q -> !selectedQuestions.contains(q))
                .collect(Collectors.toList());
        Collections.shuffle(remaining);
        int totalNeeded = request.getTotalQuestions();
        while (selectedQuestions.size() < totalNeeded && !remaining.isEmpty()) {
            selectedQuestions.add(remaining.remove(0));
        }

        if (selectedQuestions.size() < totalNeeded) {
            throw new IllegalArgumentException("Not enough questions: need " + totalNeeded +
                    " but only " + selectedQuestions.size() + " available after applying rules");
        }

        BigDecimal totalPoints = selectedQuestions.stream()
                .map(Question::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);

        QuizPaper quiz = QuizPaper.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .questionBank(bank)
                .totalQuestions(selectedQuestions.size())
                .totalPoints(totalPoints)
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1)
                .releaseStart(request.getReleaseStart())
                .releaseEnd(request.getReleaseEnd())
                .shuffleQuestions(request.getShuffleQuestions() != null && request.getShuffleQuestions())
                .showImmediateFeedback(request.getShowImmediateFeedback() == null || request.getShowImmediateFeedback())
                .createdBy(user)
                .questions(new ArrayList<>(selectedQuestions))
                .build();

        quiz = quizPaperRepository.save(quiz);

        if (request.getRules() != null) {
            for (QuizRuleDto ruleDto : request.getRules()) {
                QuizRule rule = QuizRule.builder()
                        .quizPaper(quiz).ruleType(ruleDto.getRuleType())
                        .minCount(ruleDto.getMinCount()).maxCount(ruleDto.getMaxCount())
                        .difficultyLevel(ruleDto.getDifficultyLevel()).build();
                quizRuleRepository.save(rule);
            }
        }

        auditLogService.log(userId, AuditAction.QUIZ_CREATE, "QuizPaper", quiz.getId(),
                "Assembled quiz: " + quiz.getTitle() + " with " + selectedQuestions.size() + " questions", null, null);
        return quiz;
    }

    public QuizPaper getQuiz(Long id) {
        return quizPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + id));
    }

    public Map<String, Object> getQuizForStudent(Long id) {
        QuizPaper quiz = getQuiz(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", quiz.getId());
        result.put("title", quiz.getTitle());
        result.put("description", quiz.getDescription());
        result.put("totalQuestions", quiz.getTotalQuestions());
        result.put("totalPoints", quiz.getTotalPoints());
        result.put("timeLimitMinutes", quiz.getTimeLimitMinutes());
        result.put("maxAttempts", quiz.getMaxAttempts());
        result.put("releaseStart", quiz.getReleaseStart());
        result.put("releaseEnd", quiz.getReleaseEnd());
        result.put("shuffleQuestions", quiz.getShuffleQuestions());
        result.put("showImmediateFeedback", quiz.getShowImmediateFeedback());

        List<Map<String, Object>> sanitizedQuestions = new ArrayList<>();
        for (Question q : quiz.getQuestions()) {
            Map<String, Object> qMap = new LinkedHashMap<>();
            qMap.put("id", q.getId());
            qMap.put("questionType", q.getQuestionType());
            qMap.put("difficultyLevel", q.getDifficultyLevel());
            qMap.put("questionText", q.getQuestionText());
            qMap.put("options", q.getOptions());
            qMap.put("points", q.getPoints());
            // correctAnswer and explanation are intentionally omitted for students
            sanitizedQuestions.add(qMap);
        }
        result.put("questions", sanitizedQuestions);
        return result;
    }

    public List<QuizPaper> listQuizzes(Long userId) {
        return quizPaperRepository.findByCreatedById(userId);
    }

    @Transactional
    public QuizPaper scheduleQuiz(Long id, LocalDateTime releaseStart, LocalDateTime releaseEnd, Long userId) {
        QuizPaper quiz = getQuiz(id);
        if (!quiz.getCreatedBy().getId().equals(userId)) throw new ForbiddenException("Not authorized");
        quiz.setReleaseStart(releaseStart);
        quiz.setReleaseEnd(releaseEnd);
        auditLogService.log(userId, AuditAction.QUIZ_SCHEDULE, "QuizPaper", id, "Scheduled quiz release window", null, null);
        return quizPaperRepository.save(quiz);
    }

    @Transactional
    public QuizPaper publishQuiz(Long id, Long userId) {
        QuizPaper quiz = getQuiz(id);
        if (!quiz.getCreatedBy().getId().equals(userId)) throw new ForbiddenException("Not authorized");
        quiz.setIsPublished(true);
        auditLogService.log(userId, AuditAction.QUIZ_PUBLISH, "QuizPaper", id, "Published quiz", null, null);
        return quizPaperRepository.save(quiz);
    }
}
