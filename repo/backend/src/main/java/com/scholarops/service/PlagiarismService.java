package com.scholarops.service;

import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.entity.*;
import com.scholarops.repository.*;
import com.scholarops.util.FingerprintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PlagiarismService {
    private static final Logger logger = LoggerFactory.getLogger(PlagiarismService.class);
    private final PlagiarismCheckRepository checkRepository;
    private final PlagiarismMatchRepository matchRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnswerRepository answerRepository;
    private final StandardizedContentRecordRepository contentRepository;

    @Value("${scholarops.plagiarism.similarity-threshold:0.85}")
    private double similarityThreshold;

    @Value("${scholarops.plagiarism.ngram-size:5}")
    private int ngramSize;

    @Value("${scholarops.plagiarism.window-size:4}")
    private int windowSize;

    public PlagiarismService(PlagiarismCheckRepository checkRepository, PlagiarismMatchRepository matchRepository,
            SubmissionRepository submissionRepository, SubmissionAnswerRepository answerRepository,
            StandardizedContentRecordRepository contentRepository) {
        this.checkRepository = checkRepository;
        this.matchRepository = matchRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.contentRepository = contentRepository;
    }

    @Transactional
    public PlagiarismCheck checkSubmission(Submission submission) {
        PlagiarismCheck check = PlagiarismCheck.builder().submission(submission).status("RUNNING").build();
        check = checkRepository.save(check);

        String submissionText = buildSubmissionText(submission);
        Set<Long> sourceFingerprint = FingerprintUtil.generateFingerprint(submissionText, ngramSize, windowSize);

        double maxSimilarity = 0.0;

        List<Submission> priorSubmissions = submissionRepository.findByQuizPaperId(
                submission.getQuizPaper().getId());
        for (Submission prior : priorSubmissions) {
            if (prior.getId().equals(submission.getId())) continue;
            String priorText = buildSubmissionText(prior);
            Set<Long> priorFp = FingerprintUtil.generateFingerprint(priorText, ngramSize, windowSize);
            double similarity = FingerprintUtil.computeSimilarity(sourceFingerprint, priorFp);

            if (similarity >= similarityThreshold) {
                PlagiarismMatch match = PlagiarismMatch.builder()
                        .plagiarismCheck(check).matchedSubmission(prior)
                        .similarityScore(BigDecimal.valueOf(similarity))
                        .matchedTextExcerpt(priorText.substring(0, Math.min(200, priorText.length())))
                        .sourceTextExcerpt(submissionText.substring(0, Math.min(200, submissionText.length())))
                        .build();
                matchRepository.save(match);
            }
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        List<StandardizedContentRecord> contentRecords = contentRepository.findByIsPublished(true);
        for (StandardizedContentRecord content : contentRecords) {
            if (content.getBodyText() == null) continue;
            Set<Long> contentFp = FingerprintUtil.generateFingerprint(content.getBodyText(), ngramSize, windowSize);
            double similarity = FingerprintUtil.computeSimilarity(sourceFingerprint, contentFp);

            if (similarity >= similarityThreshold) {
                PlagiarismMatch match = PlagiarismMatch.builder()
                        .plagiarismCheck(check).matchedContent(content)
                        .similarityScore(BigDecimal.valueOf(similarity))
                        .matchedTextExcerpt(content.getBodyText().substring(0, Math.min(200, content.getBodyText().length())))
                        .sourceTextExcerpt(submissionText.substring(0, Math.min(200, submissionText.length())))
                        .build();
                matchRepository.save(match);
            }
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        check.setMaxSimilarityScore(BigDecimal.valueOf(maxSimilarity));
        check.setIsFlagged(maxSimilarity >= similarityThreshold);
        check.setStatus("COMPLETED");
        check.setCheckedAt(LocalDateTime.now());
        return checkRepository.save(check);
    }

    public PlagiarismCheck getCheck(Long id) {
        return checkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plagiarism check not found: " + id));
    }

    public List<PlagiarismMatch> getMatches(Long checkId) {
        return matchRepository.findByPlagiarismCheckId(checkId);
    }

    public List<PlagiarismCheck> getFlaggedChecks() {
        return checkRepository.findByIsFlagged(true);
    }

    public List<PlagiarismCheck> getAllChecks() {
        return checkRepository.findAll();
    }

    private String buildSubmissionText(Submission submission) {
        StringBuilder sb = new StringBuilder();
        for (SubmissionAnswer answer : submission.getAnswers()) {
            if (answer.getAnswerText() != null) sb.append(answer.getAnswerText()).append(" ");
        }
        return sb.toString().trim();
    }
}
