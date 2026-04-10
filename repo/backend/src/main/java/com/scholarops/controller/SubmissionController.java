package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.AutosaveRequest;
import com.scholarops.model.entity.Submission;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/quizzes/{quizId}/submissions")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'QUIZ_TAKE')")
    public ResponseEntity<ApiResponse<Submission>> startSubmission(@PathVariable Long quizId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Submission submission = submissionService.startSubmission(quizId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(submission));
    }

    @PutMapping("/submissions/{id}/autosave")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'QUIZ_TAKE')")
    public ResponseEntity<ApiResponse<Void>> autosave(@PathVariable Long id,
            @Valid @RequestBody AutosaveRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        submissionService.autosave(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/submissions/{id}/submit")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'QUIZ_TAKE')")
    public ResponseEntity<ApiResponse<Submission>> submitSubmission(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.submitSubmission(id, currentUser.getId())));
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("(hasRole('STUDENT') and hasPermission(null, 'SUBMISSION_VIEW_OWN')) or ((hasRole('INSTRUCTOR') or hasRole('TEACHING_ASSISTANT')) and hasPermission(null, 'SUBMISSION_VIEW_ALL'))")
    public ResponseEntity<ApiResponse<Submission>> getSubmission(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isInstructor = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_INSTRUCTOR".equals(a.getAuthority()));
        boolean isTeachingAssistant = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEACHING_ASSISTANT".equals(a.getAuthority()));
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.getSubmission(id, currentUser.getId(), isInstructor, isTeachingAssistant)));
    }

    @GetMapping("/submissions/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT') and hasPermission(null, 'SUBMISSION_VIEW_OWN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedback(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                submissionService.getFeedback(id, currentUser.getId())));
    }
}
