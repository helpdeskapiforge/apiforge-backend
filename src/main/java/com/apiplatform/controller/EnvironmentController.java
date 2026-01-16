package com.apiplatform.controller;

import com.apiplatform.model.Environment;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.EnvironmentRepository;
import com.apiplatform.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    @Autowired EnvironmentRepository environmentRepository;
    @Autowired WorkspaceRepository workspaceRepository;

    // GET all environments for a workspace
    @GetMapping("/workspace/{workspaceId}")
    public List<Environment> getByWorkspace(@PathVariable Long workspaceId) {
        return environmentRepository.findByWorkspaceId(workspaceId);
    }

    // POST create new environment
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody EnvironmentReq request) {
        Workspace ws = workspaceRepository.findById(request.workspaceId)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));

        Environment env = new Environment();
        env.setName(request.name);
        env.setVariables(request.variables); // Expecting valid JSON string
        env.setWorkspace(ws);

        return ResponseEntity.ok(environmentRepository.save(env));
    }

    // PUT update existing environment (The missing piece!)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EnvironmentReq request) {
        Environment env = environmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Environment not found"));

        // Update fields
        if (request.name != null) env.setName(request.name);
        if (request.variables != null) env.setVariables(request.variables);

        return ResponseEntity.ok(environmentRepository.save(env));
    }

    // DELETE environment
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        environmentRepository.deleteById(id);
        return ResponseEntity.ok("Environment deleted successfully");
    }

    // Simple DTO
    public static class EnvironmentReq {
        public String name;
        public String variables;
        public Long workspaceId;
    }
}