package com.scholarops.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingRequest {

    @NotNull(message = "Submission answer ID is required")
    private UUID submissionAnswerId;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be non-negative")
    private Double score;

    @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
    private String feedback;
}
