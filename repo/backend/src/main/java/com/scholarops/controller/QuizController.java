package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.QuizAssemblyRequest;
import com.scholarops.model.entity.QuizPaper;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.QuizAssemblyService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizAssemblyService quizAssemblyService;

    public QuizController(QuizAssemblyService quizAssemblyService) {
        this.quizAssemblyService = quizAssemblyService;
    }

    @PostMapping("/assemble")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUIZ_MANAGE')")
    public ResponseEntity<ApiResponse<QuizPaper>> assembleQuiz(
            @Valid @RequestBody QuizAssemblyRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        QuizPaper quiz = quizAssemblyService.assembleQuiz(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(quiz));
    }

    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUIZ_MANAGE')")
    public ResponseEntity<ApiResponse<List<QuizPaper>>> listQuizzes(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(quizAssemblyService.listQuizzes(currentUser.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("(hasRole('INSTRUCTOR') and hasPermission(null, 'QUIZ_MANAGE')) or (hasRole('STUDENT') and hasPermission(null, 'QUIZ_TAKE'))")
    public ResponseEntity<?> getQuiz(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isStudent = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            return ResponseEntity.ok(ApiResponse.success(quizAssemblyService.getQuizForStudent(id)));
        }
        return ResponseEntity.ok(ApiResponse.success(quizAssemblyService.getQuiz(id)));
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUIZ_MANAGE')")
    public ResponseEntity<ApiResponse<QuizPaper>> scheduleQuiz(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LocalDateTime releaseStart = LocalDateTime.parse(body.get("releaseStart"));
        LocalDateTime releaseEnd = LocalDateTime.parse(body.get("releaseEnd"));
        return ResponseEntity.ok(ApiResponse.success(
                quizAssemblyService.scheduleQuiz(id, releaseStart, releaseEnd, currentUser.getId())));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR') and hasPermission(null, 'QUIZ_MANAGE')")
    public ResponseEntity<ApiResponse<QuizPaper>> publishQuiz(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                quizAssemblyService.publishQuiz(id, currentUser.getId())));
    }
}
