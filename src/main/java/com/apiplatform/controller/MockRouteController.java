package com.apiplatform.controller;

import com.apiplatform.model.MockRoute;
import com.apiplatform.model.MockServer;
import com.apiplatform.repository.MockRouteRepository;
import com.apiplatform.repository.MockServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/mocks/routes")
public class MockRouteController {

    @Autowired MockRouteRepository mockRouteRepository;
    @Autowired MockServerRepository mockServerRepository;

    @GetMapping("/{id}")
    public ResponseEntity<?> getRouteById(@PathVariable Long id) {
        MockRoute route = mockRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        return ResponseEntity.ok(route);
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<?> getRoutes(@PathVariable Long serverId) {
        return ResponseEntity.ok(mockRouteRepository.findByMockServerId(serverId));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoute(@RequestBody RouteRequestDto request) {
        if (request.getMockServerId() == null) {
            return ResponseEntity.badRequest().body("Error: mockServerId is required");
        }

        MockServer server = mockServerRepository.findById(request.getMockServerId())
                .orElseThrow(() -> new RuntimeException("Server not found"));

        MockRoute route = new MockRoute();
        route.setMockServer(server);

        // Defaults for new route
        route.setEnabled(true);
        route.setChaosEnabled(false);
        route.setFailureRate(0.0);

        mapDtoToRoute(request, route);

        return ResponseEntity.ok(mockRouteRepository.save(route));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @RequestBody RouteRequestDto request) {
        MockRoute route = mockRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        mapDtoToRoute(request, route);
        return ResponseEntity.ok(mockRouteRepository.save(route));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        mockRouteRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // ✅ FIXED MAPPING LOGIC
    private void mapDtoToRoute(RouteRequestDto dto, MockRoute route) {
        if (dto.getMethod() != null) route.setMethod(dto.getMethod());
        if (dto.getPath() != null) route.setPath(dto.getPath());
        if (dto.getStatusCode() != null) route.setStatusCode(dto.getStatusCode());
        if (dto.getContentType() != null) route.setContentType(dto.getContentType());
        if (dto.getResponseBody() != null) route.setResponseBody(dto.getResponseBody());
        if (dto.getResponseHeaders() != null) route.setResponseHeaders(dto.getResponseHeaders());
        if (dto.getDelayMs() != null) route.setDelayMs(dto.getDelayMs());

        // ✅ CRITICAL FIX: explicit null checks for Booleans
        if (dto.getIsEnabled() != null) route.setEnabled(dto.getIsEnabled());
        if (dto.getChaosEnabled() != null) route.setChaosEnabled(dto.getChaosEnabled());
        if (dto.getFailureRate() != null) route.setFailureRate(dto.getFailureRate());
    }

    @lombok.Data
    static class RouteRequestDto {
        private Long mockServerId;
        private String method;
        private String path;
        private Integer statusCode; // Changed int -> Integer
        private String contentType;
        private String responseBody;
        private String responseHeaders;
        private Integer delayMs;    // Changed int -> Integer

        // ✅ Changed boolean -> Boolean (Wrapper) to allow null checks
        private Boolean isEnabled;
        private Boolean chaosEnabled;
        private Double failureRate;    // Changed double -> Double
    }
}