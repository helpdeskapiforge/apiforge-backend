package com.apiplatform.web.dto.response;

public record RequestItemResponse(
        Long id,
        String name,
        String method,
        String url,
        String headers,
        String body,
        String authConfig,
        Long collectionId,
        Long workspaceId
) {
}
