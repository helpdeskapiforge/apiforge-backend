package com.apiplatform.web.dto.response;

public record CollectionResponse(Long id, String name, String description, Long workspaceId, Long parentId, Integer sortOrder) {
}
