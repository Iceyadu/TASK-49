package com.scholarops.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAssemblyRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Question bank ID is required")
    private Long questionBankId;

    @NotNull(message = "Total questions is required")
    @Min(value = 1, message = "Total questions must be at least 1")
    private Integer totalQuestions;

    @Valid
    private List<QuizRuleDto> rules;

    @Min(value = 1, message = "Time limit must be at least 1 minute")
    private Integer timeLimitMinutes;

    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts;

    private LocalDateTime releaseStart;

    private LocalDateTime releaseEnd;

    private Boolean shuffleQuestions;

    private Boolean showImmediateFeedback;
}
