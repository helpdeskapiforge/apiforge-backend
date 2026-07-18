package com.apiplatform.controller;

import com.apiplatform.model.Collection;
import com.apiplatform.model.User;
import com.apiplatform.payload.request.CollectionRequest;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.CollectionService;
import com.apiplatform.web.dto.response.CollectionResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/collections", "/api/collections"}) // legacy "/api/collections" kept temporarily; see CHANGELOG.md
public class CollectionController {

    private final CollectionService collectionService;
    private final CurrentUserProvider currentUserProvider;

    public CollectionController(CollectionService collectionService, CurrentUserProvider currentUserProvider) {
        this.collectionService = collectionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<CollectionResponse>> getCollectionsByWorkspace(@PathVariable Long workspaceId) {
        User user = currentUserProvider.getCurrentUser();
        List<CollectionResponse> response = collectionService.getCollectionsForWorkspace(workspaceId, user).stream()
                .map(ResponseMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<CollectionResponse> createCollection(@Valid @RequestBody CollectionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Collection created = collectionService.createCollection(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> updateCollection(@PathVariable Long id, @Valid @RequestBody CollectionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Collection updated = collectionService.updateCollection(id, request, user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        collectionService.deleteCollection(id, user);
        return ResponseEntity.noContent().build();
    }
}
