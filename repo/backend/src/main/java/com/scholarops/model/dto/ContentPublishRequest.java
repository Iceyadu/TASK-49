package com.scholarops.model.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class ContentPublishRequest {

    @NotNull(message = "Content record IDs are required")
    @NotEmpty(message = "Content record IDs must not be empty")
    private List<Long> contentRecordIds;
}
