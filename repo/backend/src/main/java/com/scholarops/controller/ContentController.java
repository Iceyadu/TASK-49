package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.ContentPublishRequest;
import com.scholarops.model.entity.MediaMetadata;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.service.ContentStandardizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentStandardizationService contentService;

    public ContentController(ContentStandardizationService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CONTENT_REVIEW')")
    public ResponseEntity<ApiResponse<Page<StandardizedContentRecord>>> listContent(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contentService.listContent(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CONTENT_REVIEW')")
    public ResponseEntity<ApiResponse<StandardizedContentRecord>> getContent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getContentById(id)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CONTENT_REVIEW')")
    public ResponseEntity<ApiResponse<Void>> publishContent(@PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        contentService.publishContent(List.of(id), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/publish-batch")
    @PreAuthorize("hasRole('CONTENT_CURATOR') and hasPermission(null, 'CONTENT_REVIEW')")
    public ResponseEntity<ApiResponse<Void>> publishBatch(@Valid @RequestBody ContentPublishRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        contentService.publishContent(request.getContentRecordIds(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/media-metadata/{contentId}")
    @PreAuthorize("(hasRole('CONTENT_CURATOR') and hasPermission(null, 'CONTENT_REVIEW')) or hasPermission(null, 'CONTENT_VIEW')")
    public ResponseEntity<ApiResponse<List<MediaMetadata>>> getMediaMetadata(@PathVariable Long contentId) {
        return ResponseEntity.ok(ApiResponse.success(contentService.getMediaMetadata(contentId)));
    }
}
