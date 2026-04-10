package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.CrawlRuleRequest;
import com.scholarops.model.dto.ExtractionTestRequest;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.CrawlRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CrawlRuleController {

    private final CrawlRuleService crawlRuleService;

    public CrawlRuleController(CrawlRuleService crawlRuleService) {
        this.crawlRuleService = crawlRuleService;
    }

    @GetMapping("/crawl-sources/{sourceId}/rules")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<List<CrawlRuleVersion>>> getRulesForSource(@PathVariable Long sourceId) {
        return ResponseEntity.ok(ApiResponse.success(crawlRuleService.getRulesForSource(sourceId)));
    }

    @PostMapping("/crawl-sources/{sourceId}/rules")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlRuleVersion>> createRule(@PathVariable Long sourceId,
            @Valid @RequestBody CrawlRuleRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CrawlRuleVersion rule = crawlRuleService.createRule(sourceId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(rule));
    }

    @GetMapping("/crawl-rules/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlRuleVersion>> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(crawlRuleService.getRule(id)));
    }

    @PostMapping("/crawl-rules/{id}/revert/{versionId}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlRuleVersion>> revertToVersion(@PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                crawlRuleService.revertToVersion(id, versionId, currentUser.getId())));
    }

    @PostMapping("/crawl-rules/test-extraction")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> testExtraction(
            @Valid @RequestBody ExtractionTestRequest request) {
        return ResponseEntity.ok(ApiResponse.success(crawlRuleService.testExtraction(request)));
    }
}
