package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurlGenerationRequest(
        @NotBlank(message = "description is required.")
        @Size(max = 2000, message = "description must be at most 2000 characters.")
        String description,

        @Size(max = 500)
        String baseUrl,

        @Size(max = 500)
        String authHint
) {
}
