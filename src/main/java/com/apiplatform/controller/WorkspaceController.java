package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.repository.UserRepository;
import com.apiplatform.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired UserRepository userRepository;

    @GetMapping("/my-workspaces")
    public List<Workspace> getMyWorkspaces() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return workspaceRepository.findByOwnerId(user.getId());
    }

    // 👇 ADD THIS METHOD 👇
    @PostMapping("/create")
    public ResponseEntity<?> createWorkspace(@RequestBody WorkspaceRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        Workspace ws = new Workspace();
        ws.setName(request.name);
        ws.setOwner(user);

        return ResponseEntity.ok(workspaceRepository.save(ws));
    }

    // DTO
    public static class WorkspaceRequest {
        public String name;
    }
}