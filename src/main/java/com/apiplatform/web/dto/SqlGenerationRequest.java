package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SqlGenerationRequest(
        @NotBlank(message = "description is required.")
        @Size(max = 2000, message = "description must be at most 2000 characters.")
        String description,

        /** One of MySQL, PostgreSQL, SQLite. Defaults to PostgreSQL. */
        @Pattern(regexp = "(?i)mysql|postgresql|sqlite", message = "dialect must be one of MySQL, PostgreSQL, SQLite.")
        String dialect
) {
}
