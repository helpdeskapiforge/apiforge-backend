package com.apiplatform.service;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.WorkspaceRepository;
import com.apiplatform.security.access.OwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional(readOnly = true)
    public List<Workspace> getWorkspacesForUser(User user) {
        return workspaceRepository.findByOwnerId(user.getId());
    }

    /**
     * Loads a workspace and verifies the given user owns it.
     * Every other service that needs to check "does this user have access to workspace X"
     * should call this instead of hitting the repository directly.
     */
    @Transactional(readOnly = true)
    public Workspace getOwnedWorkspace(Long workspaceId, User user) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Workspace", workspaceId));
        OwnershipGuard.assertOwnsWorkspace(workspace, user);
        return workspace;
    }

    public Workspace createWorkspace(String name, User owner) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name must not be blank.");
        }
        Workspace workspace = new Workspace();
        workspace.setName(name.trim());
        workspace.setOwner(owner);
        return workspaceRepository.save(workspace);
    }

    public Workspace renameWorkspace(Long workspaceId, String name, User user) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name must not be blank.");
        }
        Workspace workspace = getOwnedWorkspace(workspaceId, user);
        workspace.setName(name.trim());
        return workspaceRepository.save(workspace);
    }

    public void deleteWorkspace(Long workspaceId, User user) {
        Workspace workspace = getOwnedWorkspace(workspaceId, user);
        workspaceRepository.delete(workspace);
    }
}
