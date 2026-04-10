package com.scholarops.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntryRequest {

    @NotNull(message = "Schedule ID is required")
    private UUID scheduleId;

    @NotNull(message = "New start time is required")
    private LocalDateTime newStartTime;

    @NotNull(message = "New end time is required")
    private LocalDateTime newEndTime;
}
