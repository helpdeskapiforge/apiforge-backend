package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.Collection;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.CollectionRequest;
import com.apiplatform.repository.CollectionRepository;
import com.apiplatform.security.access.OwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CollectionService {

    /** Guards against a pathological/malicious deeply-nested folder chain. */
    private static final int MAX_FOLDER_DEPTH = 25;

    private final CollectionRepository collectionRepository;
    private final WorkspaceService workspaceService;

    public CollectionService(CollectionRepository collectionRepository, WorkspaceService workspaceService) {
        this.collectionRepository = collectionRepository;
        this.workspaceService = workspaceService;
    }

    /**
     * Returns the flat list of every collection/folder in the workspace, ordered for
     * stable display. The frontend builds the tree from parentId — no recursive query
     * needed here, and it keeps this endpoint a single, cheap round trip regardless of
     * how deep the tree gets.
     */
    @Transactional(readOnly = true)
    public List<Collection> getCollectionsForWorkspace(Long workspaceId, User user) {
        workspaceService.getOwnedWorkspace(workspaceId, user); // throws 404/403 as appropriate
        return collectionRepository.findByWorkspaceIdOrderBySortOrderAscIdAsc(workspaceId);
    }

    public Collection createCollection(CollectionRequest request, User user) {
        if (request.getWorkspaceId() == null) {
            throw new IllegalArgumentException("workspaceId is required.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required.");
        }
        Workspace workspace = workspaceService.getOwnedWorkspace(request.getWorkspaceId(), user);

        Collection collection = new Collection();
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setWorkspace(workspace);
        collection.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        if (request.getParentId() != null) {
            Collection parent = getOwnedCollection(request.getParentId(), user);
            assertSameWorkspace(parent, workspace);
            collection.setParentId(parent.getId());
        }

        return collectionRepository.save(collection);
    }

    public Collection updateCollection(Long id, CollectionRequest request, User user) {
        Collection collection = getOwnedCollection(id, user);

        if (request.getName() != null && !request.getName().isBlank()) {
            collection.setName(request.getName());
        }
        if (request.getDescription() != null) {
            collection.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            collection.setSortOrder(request.getSortOrder());
        }

        if (Boolean.TRUE.equals(request.getClearParent())) {
            collection.setParentId(null);
        } else if (request.getParentId() != null) {
            moveToParent(collection, request.getParentId(), user);
        }

        return collectionRepository.save(collection);
    }

    public void deleteCollection(Long id, User user) {
        Collection collection = getOwnedCollection(id, user);
        // Children point at this row via parentId with ON DELETE CASCADE at the DB
        // level (see V2 migration), so this cascades to sub-folders automatically.
        collectionRepository.delete(collection);
    }

    @Transactional(readOnly = true)
    public Collection getOwnedCollection(Long id, User user) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Collection", id));
        OwnershipGuard.assertOwnsWorkspace(collection.getWorkspace(), user);
        return collection;
    }

    private void moveToParent(Collection collection, Long newParentId, User user) {
        if (newParentId.equals(collection.getId())) {
            throw new IllegalArgumentException("A folder cannot be its own parent.");
        }

        Collection newParent = getOwnedCollection(newParentId, user);
        assertSameWorkspace(newParent, collection.getWorkspace());
        assertNoCycle(collection.getId(), newParent, user);

        collection.setParentId(newParent.getId());
    }

    /** Walk up the new parent's ancestry; reject if we ever encounter the collection being moved. */
    private void assertNoCycle(Long movingId, Collection newParent, User user) {
        Set<Long> visited = new HashSet<>();
        Collection current = newParent;
        int depth = 0;

        while (current != null) {
            if (depth++ > MAX_FOLDER_DEPTH) {
                throw new IllegalArgumentException("Folder nesting is too deep (max " + MAX_FOLDER_DEPTH + " levels).");
            }
            if (current.getId().equals(movingId)) {
                throw new IllegalArgumentException("Cannot move a folder into one of its own descendants.");
            }
            if (!visited.add(current.getId())) {
                // Defensive: data already cyclic somehow -- stop instead of looping forever.
                throw new IllegalArgumentException("Detected a cycle in the existing folder structure.");
            }
            if (current.getParentId() == null) {
                break;
            }
            current = collectionRepository.findById(current.getParentId()).orElse(null);
        }
    }

    private void assertSameWorkspace(Collection candidate, Workspace expectedWorkspace) {
        if (!candidate.getWorkspace().getId().equals(expectedWorkspace.getId())) {
            throw new IllegalArgumentException("The parent folder must belong to the same workspace.");
        }
    }
}
