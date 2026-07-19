package com.apiplatform.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MockDataRequest(
        @NotBlank(message = "description is required (e.g. 'a user with id, name, email' or a JSON example).")
        @Size(max = 4000, message = "description must be at most 4000 characters.")
        String description,

        /** One of SIMPLE, NESTED, LARGE, EDGE_CASES, INVALID, REALISTIC. Defaults to SIMPLE. */
        String mode,

        @Min(1) @Max(100)
        Integer count
) {
}
