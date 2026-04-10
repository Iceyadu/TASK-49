package com.scholarops.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogSearchRequest {

    @Size(max = 200, message = "Keyword must not exceed 200 characters")
    private String keyword;

    private String contentType;

    @Min(value = 0, message = "Min price must be non-negative")
    private BigDecimal minPrice;

    @Min(value = 0, message = "Max price must be non-negative")
    private BigDecimal maxPrice;

    private LocalDateTime availabilityStart;

    private LocalDateTime availabilityEnd;

    private String sortBy;

    private String sortDirection;

    @Min(value = 0, message = "Page must be non-negative")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Builder.Default
    private Integer size = 20;
}
