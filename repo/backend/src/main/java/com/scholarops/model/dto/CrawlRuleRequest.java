package com.scholarops.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlRuleRequest {

    @NotBlank(message = "Extraction method is required")
    private String extractionMethod;

    @NotNull(message = "Rule definition is required")
    private Map<String, Object> ruleDefinition;

    private Map<String, String> fieldMappings;

    private Map<String, String> typeValidations;

    private String notes;
}
