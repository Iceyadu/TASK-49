package com.scholarops.controller;

import com.scholarops.model.dto.ApiResponse;
import com.scholarops.model.dto.CatalogSearchRequest;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CONTENT_VIEW')")
    public ResponseEntity<ApiResponse<Page<StandardizedContentRecord>>> searchCatalog(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) LocalDateTime availabilityStart,
            @RequestParam(required = false) LocalDateTime availabilityEnd,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .keyword(keyword).contentType(contentType)
                .minPrice(minPrice).maxPrice(maxPrice)
                .availabilityStart(availabilityStart).availabilityEnd(availabilityEnd)
                .sortBy(sortBy).sortDirection(sortDirection)
                .page(page).size(size)
                .build();
        return ResponseEntity.ok(ApiResponse.success(catalogService.search(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CONTENT_VIEW')")
    public ResponseEntity<ApiResponse<StandardizedContentRecord>> getItem(@PathVariable Long id) {
        catalogService.incrementPopularity(id);
        return ResponseEntity.ok(ApiResponse.success(catalogService.getPublishedContentById(id)));
    }
}
