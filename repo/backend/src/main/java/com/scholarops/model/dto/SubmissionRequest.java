package com.scholarops.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRequest {

    @NotNull(message = "Quiz paper ID is required")
    private UUID quizPaperId;
}
