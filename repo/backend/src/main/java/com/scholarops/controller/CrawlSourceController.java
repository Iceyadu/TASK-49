package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.CrawlSourceRequest;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.CrawlSourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crawl-sources")
public class CrawlSourceController {

    private final CrawlSourceService crawlSourceService;

    public CrawlSourceController(CrawlSourceService crawlSourceService) {
        this.crawlSourceService = crawlSourceService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_SOURCE_MANAGE')")
    public ResponseEntity<ApiResponse<List<CrawlSourceProfile>>> listSources(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(crawlSourceService.listSources(currentUser.getId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_SOURCE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlSourceProfile>> createSource(
            @Valid @RequestBody CrawlSourceRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        CrawlSourceProfile source = crawlSourceService.createSource(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(source));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_SOURCE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlSourceProfile>> getSource(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(crawlSourceService.getSource(id, currentUser.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_SOURCE_MANAGE')")
    public ResponseEntity<ApiResponse<CrawlSourceProfile>> updateSource(@PathVariable Long id,
            @Valid @RequestBody CrawlSourceRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                crawlSourceService.updateSource(id, request, currentUser.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CRAWL_SOURCE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteSource(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        crawlSourceService.deleteSource(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
