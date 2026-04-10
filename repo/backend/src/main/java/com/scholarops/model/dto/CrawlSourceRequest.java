package com.scholarops.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawlSourceRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private String description;

    @Min(value = 1, message = "Rate limit must be at least 1 per minute")
    private Integer rateLimitPerMinute;

    private Boolean requiresAuth;

    private String username;

    private String password;

    private String apiKey;
}
