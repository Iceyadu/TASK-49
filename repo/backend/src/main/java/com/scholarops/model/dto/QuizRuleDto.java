package com.scholarops.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizRuleDto {

    @NotBlank(message = "Rule type is required")
    private String ruleType;

    @Min(value = 0, message = "Min count must be non-negative")
    private Integer minCount;

    @Min(value = 0, message = "Max count must be non-negative")
    private Integer maxCount;

    private Integer difficultyLevel;

    private UUID tagId;
}
