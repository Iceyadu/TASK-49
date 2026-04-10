package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.entity.PlagiarismCheck;
import com.scholarops.model.entity.PlagiarismMatch;
import com.scholarops.service.PlagiarismService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plagiarism")
public class PlagiarismController {

    private final PlagiarismService plagiarismService;

    public PlagiarismController(PlagiarismService plagiarismService) {
        this.plagiarismService = plagiarismService;
    }

    @GetMapping("/checks")
    @PreAuthorize("(hasRole('INSTRUCTOR') or hasRole('TEACHING_ASSISTANT')) and hasPermission(null, 'PLAGIARISM_VIEW')")
    public ResponseEntity<ApiResponse<List<PlagiarismCheck>>> listChecks(
            @RequestParam(required = false) Boolean flaggedOnly) {
        return ResponseEntity.ok(ApiResponse.success(
                Boolean.TRUE.equals(flaggedOnly) ? plagiarismService.getFlaggedChecks()
                        : plagiarismService.getAllChecks()));
    }

    @GetMapping("/checks/{id}")
    @PreAuthorize("(hasRole('INSTRUCTOR') or hasRole('TEACHING_ASSISTANT')) and hasPermission(null, 'PLAGIARISM_VIEW')")
    public ResponseEntity<ApiResponse<PlagiarismCheck>> getCheck(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(plagiarismService.getCheck(id)));
    }

    @GetMapping("/checks/{id}/matches")
    @PreAuthorize("(hasRole('INSTRUCTOR') or hasRole('TEACHING_ASSISTANT')) and hasPermission(null, 'PLAGIARISM_VIEW')")
    public ResponseEntity<ApiResponse<List<PlagiarismMatch>>> getMatches(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(plagiarismService.getMatches(id)));
    }
}
