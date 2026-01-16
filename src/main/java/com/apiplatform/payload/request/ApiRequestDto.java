package com.apiplatform.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiRequestDto {
    @NotBlank
    private String name;
    
    @NotBlank
    private String method; // GET, POST...
    
    @NotBlank
    private String url;
    
    private String headers; // JSON String
    private String body;    // Request Body
    private String authConfig; // JSON String
    
    private Long collectionId; // Parent Collection
    private Long workspaceId;
}