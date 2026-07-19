package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ErrorExplainRequest(
        @NotBlank(message = "logText is required.")
        @Size(max = 15000, message = "logText must be at most 15000 characters.")
        String logText,

        @Size(max = 500)
        String context
) {
}
