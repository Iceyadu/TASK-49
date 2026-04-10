package com.scholarops.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleChangeRequest {

    @NotBlank(message = "Change type is required")
    private String changeType;

    private List<UUID> scheduleIds;

    private LocalDateTime splitTime;
}
