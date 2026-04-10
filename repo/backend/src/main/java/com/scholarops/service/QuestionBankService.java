package com.scholarops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.QuestionCreateRequest;
import com.scholarops.model.entity.*;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class QuestionBankService {
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final KnowledgeTagRepository knowledgeTagRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public QuestionBankService(QuestionBankRepository questionBankRepository, QuestionRepository questionRepository,
            KnowledgeTagRepository knowledgeTagRepository, UserRepository userRepository, AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.questionRepository = questionRepository;
        this.knowledgeTagRepository = knowledgeTagRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    private String optionsToJson(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of("options", options));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize question options", e);
        }
    }

    @Transactional
    public QuestionBank createBank(String name, String description, String subject, Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        QuestionBank bank = QuestionBank.builder().name(name).description(description)
                .subject(subject).createdBy(user).build();
        bank = questionBankRepository.save(bank);
        auditLogService.log(userId, AuditAction.QUESTION_CREATE, "QuestionBank", bank.getId(),
                "Created question bank: " + name, null, null);
        return bank;
    }

    public List<QuestionBank> getBanks(Long userId) {
        return questionBankRepository.findByCreatedById(userId);
    }

    public QuestionBank getBank(Long id, Long userId) {
        QuestionBank bank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found: " + id));
        if (!bank.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Not authorized to access this question bank");
        }
        return bank;
    }

    @Transactional
    public Question addQuestion(QuestionCreateRequest request, Long userId) {
        QuestionBank bank = getBank(request.getQuestionBankId(), userId);
        User user = userRepository.findById(userId).orElseThrow();

        if (request.getDifficultyLevel() < 1 || request.getDifficultyLevel() > 5) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 5");
        }

        Question question = Question.builder()
                .questionBank(bank)
                .questionType(request.getQuestionType())
                .difficultyLevel(request.getDifficultyLevel())
                .questionText(request.getQuestionText())
                .options(optionsToJson(request.getOptions()))
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .points(request.getPoints() != null ? BigDecimal.valueOf(request.getPoints()) : BigDecimal.ONE)
                .createdBy(user)
                .build();

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<KnowledgeTag> tags = new HashSet<>(knowledgeTagRepository.findAllById(request.getTagIds()));
            question.setKnowledgeTags(tags);
        }

        question = questionRepository.save(question);
        auditLogService.log(userId, AuditAction.QUESTION_CREATE, "Question", question.getId(),
                "Created question in bank " + bank.getName(), null, null);
        return question;
    }

    @Transactional
    public Question updateQuestion(Long id, QuestionCreateRequest request, Long userId) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        if (!question.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Not authorized to modify this question");
        }
        question.setQuestionType(request.getQuestionType());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setQuestionText(request.getQuestionText());
        if (request.getOptions() != null) {
            question.setOptions(optionsToJson(request.getOptions()));
        }
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setExplanation(request.getExplanation());
        if (request.getPoints() != null) {
            question.setPoints(BigDecimal.valueOf(request.getPoints()));
        }
        if (request.getTagIds() != null) {
            Set<KnowledgeTag> tags = new HashSet<>(knowledgeTagRepository.findAllById(request.getTagIds()));
            question.setKnowledgeTags(tags);
        }
        auditLogService.log(userId, AuditAction.QUESTION_UPDATE, "Question", id, "Updated question", null, null);
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long id, Long userId) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        if (!question.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Not authorized to delete this question");
        }
        questionRepository.delete(question);
        auditLogService.log(userId, AuditAction.QUESTION_DELETE, "Question", id, "Deleted question", null, null);
    }

    public List<KnowledgeTag> getAllTags() {
        return knowledgeTagRepository.findAll();
    }

    @Transactional
    public KnowledgeTag createTag(String name, String category) {
        return knowledgeTagRepository.save(KnowledgeTag.builder().name(name).category(category).build());
    }
}
