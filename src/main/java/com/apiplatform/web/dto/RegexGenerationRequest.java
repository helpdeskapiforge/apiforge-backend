package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegexGenerationRequest(
        @NotBlank(message = "description is required.")
        @Size(max = 1000, message = "description must be at most 1000 characters.")
        String description
) {
}
