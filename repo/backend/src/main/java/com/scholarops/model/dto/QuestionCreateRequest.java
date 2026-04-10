package com.scholarops.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCreateRequest {

    @NotNull(message = "Question bank ID is required")
    private Long questionBankId;

    @NotBlank(message = "Question type is required")
    private String questionType;

    @NotNull(message = "Difficulty level is required")
    @Min(value = 1, message = "Difficulty level must be at least 1")
    @Max(value = 5, message = "Difficulty level must be at most 5")
    private Integer difficultyLevel;

    @NotBlank(message = "Question text is required")
    @Size(max = 5000, message = "Question text must not exceed 5000 characters")
    private String questionText;

    private List<String> options;

    private String correctAnswer;

    @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
    private String explanation;

    @Min(value = 0, message = "Points must be non-negative")
    private Double points;

    private List<Long> tagIds;
}
