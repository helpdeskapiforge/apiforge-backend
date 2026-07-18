package com.apiplatform.controller;

import com.apiplatform.model.MockServer;
import com.apiplatform.model.User;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.MockServerService;
import com.apiplatform.web.dto.MockServerRequest;
import com.apiplatform.web.dto.response.MockServerResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/mocks/servers", "/api/mocks/servers"}) // legacy "/api/mocks/servers" kept temporarily; see CHANGELOG.md
public class MockServerController {

    private final MockServerService mockServerService;
    private final CurrentUserProvider currentUserProvider;

    public MockServerController(MockServerService mockServerService, CurrentUserProvider currentUserProvider) {
        this.mockServerService = mockServerService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockServerResponse> getServerById(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ResponseMapper.toResponse(mockServerService.getOwned(id, user)));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<MockServerResponse>> getServersByWorkspace(@PathVariable Long workspaceId) {
        User user = currentUserProvider.getCurrentUser();
        List<MockServerResponse> response = mockServerService.getForWorkspace(workspaceId, user).stream()
                .map(ResponseMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<MockServerResponse> createServer(@RequestBody MockServerRequest request) {
        User user = currentUserProvider.getCurrentUser();
        MockServer created = mockServerService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MockServerResponse> updateServer(@PathVariable Long id, @RequestBody MockServerRequest request) {
        User user = currentUserProvider.getCurrentUser();
        MockServer updated = mockServerService.update(id, request, user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        mockServerService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
