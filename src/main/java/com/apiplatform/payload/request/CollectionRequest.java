package com.apiplatform.payload.request;

import lombok.Data;

@Data
public class CollectionRequest {
    // Deliberately NOT @NotBlank: this DTO is shared by create (name required),
    // rename (name required), and move (only parentId/clearParent sent, name omitted
    // entirely). CollectionService.createCollection checks this explicitly for the
    // one case that actually requires it.
    private String name;

    private String description;

    // Deliberately NOT @NotNull: required for create, omitted for update/move.
    // CollectionService.createCollection checks this explicitly.
    private Long workspaceId;

    // Null = root level. Set to move a collection into a folder (another Collection).
    private Long parentId;
    private Boolean clearParent; // explicit "move to root" signal (parentId alone can't distinguish "no change" from "clear")
    private Integer sortOrder;
}
