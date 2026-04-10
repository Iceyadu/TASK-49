package com.scholarops.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutosaveRequest {

    @Valid
    @NotNull(message = "Answers list is required")
    private List<AnswerEntry> answers;

    @Min(value = 0, message = "Time remaining must be non-negative")
    private Integer timeRemainingSeconds;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnswerEntry {

        @NotNull(message = "Question ID is required")
        private Long questionId;

        private String answerText;

        private String selectedOption;
    }
}
