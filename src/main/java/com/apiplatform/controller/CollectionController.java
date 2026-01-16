package com.apiplatform.controller;

import com.apiplatform.model.Collection;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.CollectionRequest;
import com.apiplatform.repository.CollectionRepository;
import com.apiplatform.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired CollectionRepository collectionRepository;
    @Autowired WorkspaceRepository workspaceRepository;

    // GET all collections for a specific workspace
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<Collection>> getCollectionsByWorkspace(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(collectionRepository.findByWorkspaceId(workspaceId));
    }

    // POST create a new collection
    @PostMapping("/create")
    public ResponseEntity<?> createCollection(@RequestBody CollectionRequest request) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new RuntimeException("Error: Workspace not found."));

        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setWorkspace(workspace);

        collectionRepository.save(collection);

        return ResponseEntity.ok("Collection created successfully!");
    }

    // ... inside CollectionController ...

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollection(@PathVariable Long id, @RequestBody CollectionRequest request) {
        Collection col = collectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        col.setName(request.getName());
        return ResponseEntity.ok(collectionRepository.save(col));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollection(@PathVariable Long id) {
        collectionRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully");
    }
}