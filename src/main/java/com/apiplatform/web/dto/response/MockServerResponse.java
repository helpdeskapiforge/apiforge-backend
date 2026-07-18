package com.apiplatform.web.dto.response;

public record MockServerResponse(Long id, String name, Integer port, String pathPrefix, Long workspaceId) {
}
