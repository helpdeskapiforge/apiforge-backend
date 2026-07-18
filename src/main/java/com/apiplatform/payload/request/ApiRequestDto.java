package com.apiplatform.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String method; // GET, POST...

    // Deliberately NOT @NotBlank: the "New Request" dialog intentionally creates a
    // request with an empty url (filled in afterwards in the editor). Rejecting that
    // here would break the core "create request" flow.
    private String url;

    private String headers; // JSON String
    private String body;    // Request Body
    private String authConfig; // JSON String

    // Deliberately NOT @NotNull: this DTO is shared by both create (where these are
    // required) and update (where the frontend only sends the editable fields, not
    // the parent IDs). RequestItemService.createRequest checks these explicitly.
    private Long collectionId; // Parent Collection
    private Long workspaceId;
}
