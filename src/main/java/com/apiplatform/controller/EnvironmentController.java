package com.apiplatform.controller;

import com.apiplatform.model.Environment;
import com.apiplatform.model.User;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.EnvironmentService;
import com.apiplatform.web.dto.EnvironmentRequest;
import com.apiplatform.web.dto.response.EnvironmentResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/environments", "/api/environments"}) // legacy "/api/environments" kept temporarily; see CHANGELOG.md
public class EnvironmentController {

    private final EnvironmentService environmentService;
    private final CurrentUserProvider currentUserProvider;

    public EnvironmentController(EnvironmentService environmentService, CurrentUserProvider currentUserProvider) {
        this.environmentService = environmentService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<EnvironmentResponse>> getByWorkspace(@PathVariable Long workspaceId) {
        User user = currentUserProvider.getCurrentUser();
        List<EnvironmentResponse> response = environmentService.getForWorkspace(workspaceId, user).stream()
                .map(ResponseMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<EnvironmentResponse> create(@RequestBody EnvironmentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Environment created = environmentService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> update(@PathVariable Long id, @RequestBody EnvironmentRequest request) {
        User user = currentUserProvider.getCurrentUser();
        Environment updated = environmentService.update(id, request, user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        environmentService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
