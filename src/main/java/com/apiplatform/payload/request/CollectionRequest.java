package com.apiplatform.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectionRequest {
    @NotBlank
    private String name;
    
    private String description;
    
    private Long workspaceId; // The workspace ID to attach this collection to
}