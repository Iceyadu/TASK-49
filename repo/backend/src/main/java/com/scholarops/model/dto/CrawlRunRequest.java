package com.scholarops.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlRunRequest {

    @NotNull(message = "Source profile ID is required")
    private Long sourceProfileId;

    @NotNull(message = "Rule version ID is required")
    private Long ruleVersionId;
}
