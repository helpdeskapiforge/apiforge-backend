package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JsonValidateRequest(
        @NotBlank(message = "json is required.")
        @Size(max = 20000, message = "json must be at most 20000 characters.")
        String json,

        /** Optional: an example JSON showing the expected shape/types, for structural (not just syntax) checking. */
        @Size(max = 20000)
        String expectedSchema
) {
}
