package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.entity.WrongAnswerHistory;
import com.scholarops.repository.WrongAnswerHistoryRepository;
import com.scholarops.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wrong-answers")
public class WrongAnswerController {

    private final WrongAnswerHistoryRepository wrongAnswerHistoryRepository;

    public WrongAnswerController(WrongAnswerHistoryRepository wrongAnswerHistoryRepository) {
        this.wrongAnswerHistoryRepository = wrongAnswerHistoryRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'WRONG_ANSWER_VIEW_OWN')")
    public ResponseEntity<ApiResponse<List<WrongAnswerHistory>>> getWrongAnswers(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                wrongAnswerHistoryRepository.findByStudentId(currentUser.getId())));
    }

    @GetMapping("/{questionId}")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'WRONG_ANSWER_VIEW_OWN')")
    public ResponseEntity<ApiResponse<List<WrongAnswerHistory>>> getWrongAnswersForQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                wrongAnswerHistoryRepository.findByStudentIdAndQuestionId(currentUser.getId(), questionId)));
    }
}
