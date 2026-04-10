package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.GradingRequest;
import com.scholarops.model.dto.RubricScoreRequest;
import com.scholarops.model.entity.GradingState;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.GradingWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final GradingWorkflowService gradingWorkflowService;

    public GradingController(GradingWorkflowService gradingWorkflowService) {
        this.gradingWorkflowService = gradingWorkflowService;
    }

    @GetMapping("/queue")
    @PreAuthorize("(hasRole('TEACHING_ASSISTANT') or hasRole('INSTRUCTOR')) and hasPermission(null, 'GRADING_VIEW')")
    public ResponseEntity<ApiResponse<Page<GradingState>>> getGradingQueue(
            Pageable pageable, @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingWorkflowService.getGradingQueue(pageable, status)));
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("(hasRole('TEACHING_ASSISTANT') or hasRole('INSTRUCTOR')) and hasPermission(null, 'GRADING_VIEW')")
    public ResponseEntity<ApiResponse<GradingState>> getGradingState(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingWorkflowService.getGradingStateForGrader(id, currentUser.getId())));
    }

    @PostMapping("/submissions/{id}/grade")
    @PreAuthorize("(hasRole('TEACHING_ASSISTANT') or hasRole('INSTRUCTOR')) and hasPermission(null, 'GRADING_MANAGE')")
    public ResponseEntity<ApiResponse<GradingState>> gradeItem(@PathVariable Long id,
            @Valid @RequestBody GradingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingWorkflowService.gradeItem(id, request, currentUser.getId())));
    }

    @PostMapping("/submissions/{id}/rubric-scores")
    @PreAuthorize("(hasRole('TEACHING_ASSISTANT') or hasRole('INSTRUCTOR')) and hasPermission(null, 'GRADING_MANAGE')")
    public ResponseEntity<ApiResponse<GradingState>> addRubricScores(@PathVariable Long id,
            @Valid @RequestBody List<RubricScoreRequest> rubricScores,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                gradingWorkflowService.addRubricScores(id, rubricScores, currentUser.getId())));
    }
}
