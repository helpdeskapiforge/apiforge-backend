package com.apiplatform.web.dto.response;

public record EnvironmentResponse(Long id, String name, String variables, Long workspaceId) {
}
