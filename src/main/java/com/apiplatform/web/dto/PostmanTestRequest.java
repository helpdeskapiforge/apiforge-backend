package com.apiplatform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostmanTestRequest(
        @NotBlank(message = "method is required.")
        @Size(max = 10)
        String method,

        @NotBlank(message = "url is required.")
        @Size(max = 2048)
        String url,

        Integer statusCode,

        @NotBlank(message = "responseBody is required -- generate tests from an actual response.")
        @Size(max = 20000, message = "responseBody must be at most 20000 characters.")
        String responseBody
) {
}
