package com.scholarops.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricScoreRequest {

    @NotBlank(message = "Criterion name is required")
    private String criterionName;

    @NotNull(message = "Max score is required")
    @Min(value = 0, message = "Max score must be non-negative")
    private Double maxScore;

    @NotNull(message = "Awarded score is required")
    @Min(value = 0, message = "Awarded score must be non-negative")
    private Double awardedScore;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}
