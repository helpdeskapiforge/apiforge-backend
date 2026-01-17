package com.apiplatform.controller;

import com.apiplatform.model.MockServer;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.MockServerRepository;
import com.apiplatform.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/mocks/servers")
public class MockServerController {

    @Autowired MockServerRepository mockServerRepository;
    @Autowired WorkspaceRepository workspaceRepository;

    // GET Single Server by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getServerById(@PathVariable Long id) {
        MockServer server = mockServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mock Server not found with id: " + id));
        return ResponseEntity.ok(server);
    }

    // GET all mock servers in a workspace
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<MockServer>> getServersByWorkspace(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(mockServerRepository.findByWorkspaceId(workspaceId));
    }

    // POST create new server
    @PostMapping("/create")
    public ResponseEntity<?> createServer(@RequestBody ServerDto request) {
        if (request.workspaceId == null) {
            return ResponseEntity.badRequest().body("Error: workspaceId is required.");
        }

        Workspace ws = workspaceRepository.findById(request.workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));

        MockServer server = new MockServer();
        server.setName(request.name);
        server.setWorkspace(ws);
        server.setPort(8080); // Default port logic if needed

        String prefix = (request.pathPrefix == null || request.pathPrefix.isEmpty())
                ? UUID.randomUUID().toString().substring(0, 8)
                : request.pathPrefix;
        server.setPathPrefix(prefix);

        return ResponseEntity.ok(mockServerRepository.save(server));
    }

    // ✅ ADDED: PUT update existing server
    @PutMapping("/{id}")
    public ResponseEntity<?> updateServer(@PathVariable Long id, @RequestBody ServerDto request) {
        MockServer server = mockServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mock Server not found with id: " + id));

        // Update fields if provided
        if (request.name != null && !request.name.isEmpty()) {
            server.setName(request.name);
        }

        if (request.pathPrefix != null && !request.pathPrefix.isEmpty()) {
            server.setPathPrefix(request.pathPrefix);
        }

        return ResponseEntity.ok(mockServerRepository.save(server));
    }

    // DELETE server
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteServer(@PathVariable Long id) {
        mockServerRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // DTO Class
    static class ServerDto {
        public String name;
        public String pathPrefix;
        public Long workspaceId;
    }
}