package com.scholarops.controller;

import com.scholarops.model.entity.*;
import com.scholarops.repository.*;
import com.scholarops.service.AuditLogService;
import com.scholarops.service.QuizAssemblyService;
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
class QuizAnswerSecurityTest {

    @Mock private QuizPaperRepository quizPaperRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private QuizRuleRepository quizRuleRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private QuizAssemblyService quizAssemblyService;

    private QuizPaper createQuizWithQuestion() {
        Question question = Question.builder()
                .id(1L)
                .questionType("MULTIPLE_CHOICE")
                .difficultyLevel(2)
                .questionText("What is the capital of France?")
                .options("[\"Paris\",\"London\",\"Berlin\",\"Madrid\"]")
                .correctAnswer("Paris")
                .explanation("Paris is the capital and largest city of France.")
                .points(BigDecimal.valueOf(5))
                .build();

        return QuizPaper.builder()
                .id(1L)
                .title("Geography Quiz")
                .description("Test your geography knowledge")
                .totalQuestions(1)
                .totalPoints(BigDecimal.valueOf(5))
                .timeLimitMinutes(30)
                .maxAttempts(3)
                .shuffleQuestions(false)
                .showImmediateFeedback(true)
                .questions(new ArrayList<>(List.of(question)))
                .build();
    }

    @Test
    void getQuizForStudentDoesNotIncludeCorrectAnswer() {
        QuizPaper quiz = createQuizWithQuestion();
        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));

        Map<String, Object> studentView = quizAssemblyService.getQuizForStudent(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) studentView.get("questions");
        assertFalse(questions.isEmpty());

        Map<String, Object> questionMap = questions.get(0);
        assertFalse(questionMap.containsKey("correctAnswer"),
                "Student view must not contain correctAnswer");
    }

    @Test
    void getQuizForStudentDoesNotIncludeExplanation() {
        QuizPaper quiz = createQuizWithQuestion();
        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));

        Map<String, Object> studentView = quizAssemblyService.getQuizForStudent(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) studentView.get("questions");
        assertFalse(questions.isEmpty());

        Map<String, Object> questionMap = questions.get(0);
        assertFalse(questionMap.containsKey("explanation"),
                "Student view must not contain explanation");
    }

    @Test
    void getQuizForStudentIncludesQuestionTextOptionsAndPoints() {
        QuizPaper quiz = createQuizWithQuestion();
        when(quizPaperRepository.findById(1L)).thenReturn(Optional.of(quiz));

        Map<String, Object> studentView = quizAssemblyService.getQuizForStudent(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) studentView.get("questions");
        assertEquals(1, questions.size());

        Map<String, Object> questionMap = questions.get(0);
        assertEquals("What is the capital of France?", questionMap.get("questionText"));
        assertEquals("[\"Paris\",\"London\",\"Berlin\",\"Madrid\"]", questionMap.get("options"));
        assertEquals(BigDecimal.valueOf(5), questionMap.get("points"));
    }
}
