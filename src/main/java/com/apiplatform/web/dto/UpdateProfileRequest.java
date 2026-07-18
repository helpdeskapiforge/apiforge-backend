package com.apiplatform.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50) String fullName,
        @Size(min = 6, max = 40) String password
) {
}
