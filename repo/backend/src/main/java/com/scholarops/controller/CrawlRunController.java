package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.CrawlRunRequest;
import com.scholarops.model.entity.CrawlRun;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.CrawlRunService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl-runs")
public class CrawlRunController {

    private final CrawlRunService crawlRunService;

    public CrawlRunController(CrawlRunService crawlRunService) {
        this.crawlRunService = crawlRunService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RUN_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlRun>> startRun(@Valid @RequestBody CrawlRunRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CrawlRun run = crawlRunService.startRun(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(run));
    }

    @GetMapping
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RUN_MANAGE')")
    public ResponseEntity<ApiResponse<Page<CrawlRun>>> listRuns(Pageable pageable,
            @RequestParam(required = false) Long sourceId) {
        return ResponseEntity.ok(ApiResponse.success(crawlRunService.listRuns(pageable, sourceId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RUN_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlRun>> getRun(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(crawlRunService.getRun(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RUN_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> cancelRun(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        crawlRunService.cancelRun(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
