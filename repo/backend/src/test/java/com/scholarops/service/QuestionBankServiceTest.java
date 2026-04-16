package com.scholarops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.QuestionCreateRequest;
import com.scholarops.model.entity.KnowledgeTag;
import com.scholarops.model.entity.Question;
import com.scholarops.model.entity.QuestionBank;
import com.scholarops.model.entity.User;
import com.scholarops.repository.KnowledgeTagRepository;
import com.scholarops.repository.QuestionBankRepository;
import com.scholarops.repository.QuestionRepository;
import com.scholarops.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private KnowledgeTagRepository knowledgeTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private QuestionBankService questionBankService;

    private User buildUser(Long id) {
        return User.builder().id(id).username("instructor" + id)
                .email("instructor" + id + "@test.com").enabled(true).accountLocked(false).build();
    }

    private QuestionBank buildBank(Long id, Long ownerId) {
        return QuestionBank.builder().id(id).name("bank-" + id)
                .description("Bank " + id).subject("Math")
                .createdBy(buildUser(ownerId)).build();
    }

    @Test
    void createBankSuccessfully() {
        User user = buildUser(1L);
        QuestionBank saved = buildBank(10L, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionBankRepository.save(any(QuestionBank.class))).thenReturn(saved);

        QuestionBank result = questionBankService.createBank("Test Bank", "Desc", "Math", 1L);

        assertEquals(10L, result.getId());
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getBanksReturnsList() {
        List<QuestionBank> banks = List.of(buildBank(1L, 5L), buildBank(2L, 5L));
        when(questionBankRepository.findByCreatedById(5L)).thenReturn(banks);

        List<QuestionBank> result = questionBankService.getBanks(5L);

        assertEquals(2, result.size());
    }

    @Test
    void getBankByIdSuccessfully() {
        QuestionBank bank = buildBank(1L, 5L);
        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));

        QuestionBank result = questionBankService.getBank(1L, 5L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getBankThrowsForbiddenForDifferentOwner() {
        QuestionBank bank = buildBank(1L, 5L);
        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));

        assertThrows(ForbiddenException.class, () -> questionBankService.getBank(1L, 99L));
    }

    @Test
    void getBankThrowsNotFoundForMissingId() {
        when(questionBankRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionBankService.getBank(999L, 5L));
    }

    @Test
    void addQuestionSuccessfully() {
        User user = buildUser(1L);
        QuestionBank bank = buildBank(10L, 1L);

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestionBankId(10L);
        request.setQuestionType("MULTIPLE_CHOICE");
        request.setDifficultyLevel(3);
        request.setQuestionText("What is 2+2?");
        request.setCorrectAnswer("4");
        request.setOptions(List.of("2", "3", "4", "5"));
        request.setPoints(1.0);

        Question savedQuestion = Question.builder().id(100L).questionBank(bank)
                .questionText("What is 2+2?").correctAnswer("4")
                .difficultyLevel(3).points(BigDecimal.ONE)
                .createdBy(user).build();

        when(questionBankRepository.findById(10L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);

        Question result = questionBankService.addQuestion(request, 1L);

        assertEquals(100L, result.getId());
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addQuestionThrowsForInvalidDifficultyLevel() {
        QuestionBank bank = buildBank(10L, 1L);
        User user = buildUser(1L);
        when(questionBankRepository.findById(10L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestionBankId(10L);
        request.setQuestionType("MULTIPLE_CHOICE");
        request.setDifficultyLevel(6); // out of range
        request.setQuestionText("Invalid difficulty");
        request.setCorrectAnswer("x");

        assertThrows(IllegalArgumentException.class, () -> questionBankService.addQuestion(request, 1L));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateQuestionSuccessfully() {
        User user = buildUser(1L);
        QuestionBank bank = buildBank(10L, 1L);

        Question existing = Question.builder().id(50L).questionBank(bank)
                .questionText("Old text").correctAnswer("old")
                .difficultyLevel(1).points(BigDecimal.ONE)
                .createdBy(user).build();

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestionBankId(10L);
        request.setQuestionType("SHORT_ANSWER");
        request.setDifficultyLevel(2);
        request.setQuestionText("New text");
        request.setCorrectAnswer("new");

        when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question result = questionBankService.updateQuestion(50L, request, 1L);

        assertEquals("New text", result.getQuestionText());
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateQuestionThrowsForbiddenForDifferentOwner() {
        User owner = buildUser(1L);
        Question existing = Question.builder().id(50L).questionText("Q").createdBy(owner).build();

        when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

        QuestionCreateRequest request = new QuestionCreateRequest();
        request.setQuestionType("SHORT_ANSWER");
        request.setDifficultyLevel(1);
        request.setQuestionText("Hijack");
        request.setCorrectAnswer("x");

        assertThrows(ForbiddenException.class, () -> questionBankService.updateQuestion(50L, request, 99L));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void deleteQuestionSuccessfully() {
        User user = buildUser(1L);
        Question existing = Question.builder().id(50L).questionText("Q").createdBy(user).build();

        when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));
        doNothing().when(questionRepository).delete(existing);

        questionBankService.deleteQuestion(50L, 1L);

        verify(questionRepository).delete(existing);
        verify(auditLogService).log(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteQuestionThrowsForbiddenForDifferentOwner() {
        User owner = buildUser(1L);
        Question existing = Question.builder().id(50L).questionText("Q").createdBy(owner).build();

        when(questionRepository.findById(50L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class, () -> questionBankService.deleteQuestion(50L, 99L));
        verify(questionRepository, never()).delete(any());
    }

    @Test
    void getAllTagsReturnsList() {
        when(knowledgeTagRepository.findAll()).thenReturn(List.of(
                KnowledgeTag.builder().id(1L).name("algebra").category("Math").build(),
                KnowledgeTag.builder().id(2L).name("calculus").category("Math").build()));

        List<KnowledgeTag> result = questionBankService.getAllTags();

        assertEquals(2, result.size());
    }

    @Test
    void createTagSuccessfully() {
        KnowledgeTag saved = KnowledgeTag.builder().id(1L).name("geometry").category("Math").build();
        when(knowledgeTagRepository.save(any(KnowledgeTag.class))).thenReturn(saved);

        KnowledgeTag result = questionBankService.createTag("geometry", "Math");

        assertEquals("geometry", result.getName());
        assertEquals("Math", result.getCategory());
    }
}
