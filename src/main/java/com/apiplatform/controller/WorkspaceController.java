package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.WorkspaceService;
import com.apiplatform.web.dto.response.WorkspaceResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/workspaces", "/api/workspaces"}) // legacy "/api/workspaces" kept temporarily; see CHANGELOG.md
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUserProvider currentUserProvider;

    public WorkspaceController(WorkspaceService workspaceService, CurrentUserProvider currentUserProvider) {
        this.workspaceService = workspaceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/my-workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces() {
        User user = currentUserProvider.getCurrentUser();
        List<WorkspaceResponse> response = workspaceService.getWorkspacesForUser(user).stream()
                .map(ResponseMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody WorkspaceRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Workspace created = workspaceService.createWorkspace(request.name(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> renameWorkspace(@PathVariable Long id, @RequestBody WorkspaceRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Workspace updated = workspaceService.renameWorkspace(id, request.name(), user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        workspaceService.deleteWorkspace(id, user);
        return ResponseEntity.noContent().build();
    }

    public record WorkspaceRequest(String name) {
    }
}
