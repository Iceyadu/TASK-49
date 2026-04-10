package com.scholarops.service;

import com.scholarops.model.dto.QuizAssemblyRequest;
import com.scholarops.model.dto.QuizRuleDto;
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
class QuizAssemblyServiceTest {

    @Mock private QuizPaperRepository quizPaperRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private QuizRuleRepository quizRuleRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private QuizAssemblyService quizAssemblyService;

    private List<Question> createQuestions(int count, int difficultyLevel) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(Question.builder()
                    .id((long) (difficultyLevel * 100 + i))
                    .questionText("Question " + i)
                    .difficultyLevel(difficultyLevel)
                    .points(BigDecimal.ONE)
                    .build());
        }
        return questions;
    }

    @Test
    void testAssembleQuizSuccess() {
        QuestionBank bank = QuestionBank.builder().id(1L).name("Math").build();
        User user = User.builder().id(1L).username("instructor").build();
        List<Question> questions = createQuestions(10, 1);

        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Math Quiz");
        request.setTotalQuestions(5);

        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionRepository.findByQuestionBankId(1L)).thenReturn(questions);
        when(quizPaperRepository.save(any(QuizPaper.class))).thenAnswer(inv -> {
            QuizPaper q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });

        QuizPaper result = quizAssemblyService.assembleQuiz(request, 1L);

        assertNotNull(result);
        assertEquals("Math Quiz", result.getTitle());
        assertEquals(5, result.getTotalQuestions());
    }

    @Test
    void testAssembleQuizInsufficientQuestions() {
        QuestionBank bank = QuestionBank.builder().id(1L).name("Math").build();
        User user = User.builder().id(1L).username("instructor").build();
        List<Question> questions = createQuestions(3, 1);

        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Big Quiz");
        request.setTotalQuestions(10);

        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionRepository.findByQuestionBankId(1L)).thenReturn(questions);

        assertThrows(IllegalArgumentException.class,
                () -> quizAssemblyService.assembleQuiz(request, 1L));
    }

    @Test
    void testAssembleWithDifficultyRules() {
        QuestionBank bank = QuestionBank.builder().id(1L).name("Math").build();
        User user = User.builder().id(1L).username("instructor").build();

        List<Question> allQuestions = new ArrayList<>();
        allQuestions.addAll(createQuestions(5, 1)); // easy
        allQuestions.addAll(createQuestions(5, 2)); // medium
        allQuestions.addAll(createQuestions(5, 3)); // hard

        QuizRuleDto rule = new QuizRuleDto();
        rule.setRuleType("DIFFICULTY");
        rule.setDifficultyLevel(3);
        rule.setMinCount(2);

        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Mixed Quiz");
        request.setTotalQuestions(5);
        request.setRules(List.of(rule));

        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionRepository.findByQuestionBankId(1L)).thenReturn(allQuestions);
        when(quizPaperRepository.save(any(QuizPaper.class))).thenAnswer(inv -> {
            QuizPaper q = inv.getArgument(0);
            q.setId(2L);
            return q;
        });

        QuizPaper result = quizAssemblyService.assembleQuiz(request, 1L);

        assertNotNull(result);
        assertEquals(5, result.getTotalQuestions());
    }

    @Test
    void testDifficultyConstraintNotMet() {
        QuestionBank bank = QuestionBank.builder().id(1L).name("Math").build();
        User user = User.builder().id(1L).username("instructor").build();

        List<Question> allQuestions = new ArrayList<>();
        allQuestions.addAll(createQuestions(5, 1)); // easy only

        QuizRuleDto rule = new QuizRuleDto();
        rule.setRuleType("DIFFICULTY");
        rule.setDifficultyLevel(3);
        rule.setMinCount(3);

        QuizAssemblyRequest request = new QuizAssemblyRequest();
        request.setQuestionBankId(1L);
        request.setTitle("Hard Quiz");
        request.setTotalQuestions(5);
        request.setRules(List.of(rule));

        when(questionBankRepository.findById(1L)).thenReturn(Optional.of(bank));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(questionRepository.findByQuestionBankId(1L)).thenReturn(allQuestions);

        assertThrows(IllegalArgumentException.class,
                () -> quizAssemblyService.assembleQuiz(request, 1L));
    }
}
