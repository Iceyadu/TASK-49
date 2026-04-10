package com.scholarops.service;

import com.scholarops.model.entity.*;
import com.scholarops.repository.PlagiarismCheckRepository;
import com.scholarops.repository.PlagiarismMatchRepository;
import com.scholarops.repository.StandardizedContentRecordRepository;
import com.scholarops.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlagiarismServiceTest {

    @Mock private PlagiarismCheckRepository plagiarismCheckRepository;
    @Mock private PlagiarismMatchRepository plagiarismMatchRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private StandardizedContentRecordRepository standardizedContentRecordRepository;

    @InjectMocks
    private PlagiarismService plagiarismService;

    @BeforeEach
    void injectScholarOpsPlagiarismConfig() {
        ReflectionTestUtils.setField(plagiarismService, "similarityThreshold", 0.85);
        ReflectionTestUtils.setField(plagiarismService, "ngramSize", 5);
        ReflectionTestUtils.setField(plagiarismService, "windowSize", 4);
    }

    private Submission createSubmission(Long id, String answerText) {
        SubmissionAnswer answer = SubmissionAnswer.builder()
                .id(id).answerText(answerText).build();
        QuizPaper quiz = QuizPaper.builder().id(1L).build();
        return Submission.builder()
                .id(id).quizPaper(quiz)
                .answers(new ArrayList<>(List.of(answer)))
                .build();
    }

    @Test
    void testCheckSubmissionNoMatch() {
        Submission submission = createSubmission(1L,
                "This is a completely unique and original answer to the question");

        when(plagiarismCheckRepository.save(any(PlagiarismCheck.class)))
                .thenAnswer(inv -> {
                    PlagiarismCheck c = inv.getArgument(0);
                    c.setId(1L);
                    c.setMatches(new ArrayList<>());
                    return c;
                });
        when(submissionRepository.findByQuizPaperId(1L)).thenReturn(List.of(submission));
        when(standardizedContentRecordRepository.findByIsPublished(eq(true)))
                .thenReturn(Collections.emptyList());

        PlagiarismCheck result = plagiarismService.checkSubmission(submission);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertFalse(result.getIsFlagged());
    }

    @Test
    void testCheckSubmissionFlagged() {
        String identicalText = "This is the exact same answer text that both students submitted word for word as their response";
        Submission current = createSubmission(2L, identicalText);
        Submission prior = createSubmission(1L, identicalText);

        when(plagiarismCheckRepository.save(any(PlagiarismCheck.class)))
                .thenAnswer(inv -> {
                    PlagiarismCheck c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(1L);
                    if (c.getMatches() == null) c.setMatches(new ArrayList<>());
                    return c;
                });
        when(submissionRepository.findByQuizPaperId(1L))
                .thenReturn(List.of(prior, current));
        when(standardizedContentRecordRepository.findByIsPublished(eq(true)))
                .thenReturn(Collections.emptyList());

        PlagiarismCheck result = plagiarismService.checkSubmission(current);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertTrue(result.getIsFlagged());
        assertTrue(result.getMaxSimilarityScore().doubleValue() >= 0.85);
    }

    @Test
    void testSimilarityThreshold() {
        Submission current = createSubmission(2L,
                "The quick brown fox jumps over the lazy dog multiple times today");
        Submission prior = createSubmission(1L,
                "A completely different and unrelated answer about mathematics and science topics");

        when(plagiarismCheckRepository.save(any(PlagiarismCheck.class)))
                .thenAnswer(inv -> {
                    PlagiarismCheck c = inv.getArgument(0);
                    if (c.getId() == null) c.setId(1L);
                    if (c.getMatches() == null) c.setMatches(new ArrayList<>());
                    return c;
                });
        when(submissionRepository.findByQuizPaperId(1L))
                .thenReturn(List.of(prior, current));
        when(standardizedContentRecordRepository.findByIsPublished(eq(true)))
                .thenReturn(Collections.emptyList());

        PlagiarismCheck result = plagiarismService.checkSubmission(current);

        assertNotNull(result);
        assertFalse(result.getIsFlagged());
    }
}
