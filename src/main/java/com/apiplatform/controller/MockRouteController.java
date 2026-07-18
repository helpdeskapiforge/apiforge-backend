package com.apiplatform.controller;

import com.apiplatform.model.MockRoute;
import com.apiplatform.model.User;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.MockRouteService;
import com.apiplatform.web.dto.MockRouteRequest;
import com.apiplatform.web.dto.response.MockRouteResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/mocks/routes", "/api/mocks/routes"}) // legacy "/api/mocks/routes" kept temporarily; see CHANGELOG.md
public class MockRouteController {

    private final MockRouteService mockRouteService;
    private final CurrentUserProvider currentUserProvider;

    public MockRouteController(MockRouteService mockRouteService, CurrentUserProvider currentUserProvider) {
        this.mockRouteService = mockRouteService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockRouteResponse> getRouteById(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ResponseMapper.toResponse(mockRouteService.getOwned(id, user)));
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<MockRouteResponse>> getRoutes(@PathVariable Long serverId) {
        User user = currentUserProvider.getCurrentUser();
        List<MockRouteResponse> response = mockRouteService.getForServer(serverId, user).stream()
                .map(ResponseMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<MockRouteResponse> createRoute(@RequestBody MockRouteRequest request) {
        User user = currentUserProvider.getCurrentUser();
        MockRoute created = mockRouteService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MockRouteResponse> updateRoute(@PathVariable Long id, @RequestBody MockRouteRequest request) {
        User user = currentUserProvider.getCurrentUser();
        MockRoute updated = mockRouteService.update(id, request, user);
        return ResponseEntity.ok(ResponseMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        User user = currentUserProvider.getCurrentUser();
        mockRouteService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
