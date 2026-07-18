package com.apiplatform.controller;

import com.apiplatform.model.MockLog;
import com.apiplatform.model.MockRoute;
import com.apiplatform.model.MockServer;
import com.apiplatform.repository.MockLogRepository;
import com.apiplatform.repository.MockRouteRepository;
import com.apiplatform.repository.MockServerRepository;
import com.apiplatform.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Publicly reachable (see WebSecurityConfig) so consumers of a shared mock server don't
 * need an APIForge account. Because it's public, every input here is untrusted:
 * path prefix, sub-path, and method are all attacker-controlled.
 */
@RestController
@RequestMapping("/api/mock/simulator")
public class MockSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(MockSimulatorController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MockServerRepository mockServerRepository;
    private final MockRouteRepository mockRouteRepository;
    private final MockLogRepository mockLogRepository;

    public MockSimulatorController(MockServerRepository mockServerRepository,
                                    MockRouteRepository mockRouteRepository,
                                    MockLogRepository mockLogRepository) {
        this.mockServerRepository = mockServerRepository;
        this.mockRouteRepository = mockRouteRepository;
        this.mockLogRepository = mockLogRepository;
    }

    @RequestMapping(value = "/{prefix}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> handleMockRequest(@PathVariable String prefix, HttpServletRequest request) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        MockServer server = mockServerRepository.findByPathPrefix(prefix)
                .orElseThrow(() -> new ResourceNotFoundException("Mock server not found for prefix: " + prefix));

        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String subPath = fullPath.replace("/api/mock/simulator/" + prefix, "");
        if (subPath.isEmpty()) subPath = "/";

        MockRoute route = mockRouteRepository.findMatchingRoute(server.getId(), request.getMethod(), subPath)
                .orElse(null);

        if (route == null) {
            return ResponseEntity.status(404).body("No active mock route found for: " + request.getMethod() + " " + subPath);
        }

        if (route.getDelayMs() > 0) {
            Thread.sleep(route.getDelayMs());
        }

        boolean isChaosFailure = route.isChaosEnabled()
                && ThreadLocalRandom.current().nextDouble() < route.getFailureRate();

        ResponseEntity<String> responseEntity;
        if (isChaosFailure) {
            responseEntity = ResponseEntity.status(500).body("Chaos Monkey: Failure Simulated");
        } else {
            responseEntity = buildConfiguredResponse(route);
        }

        logRequest(request, subPath, server, responseEntity, isChaosFailure, startTime);
        return responseEntity;
    }

    private ResponseEntity<String> buildConfiguredResponse(MockRoute route) {
        HttpHeaders headers = new HttpHeaders();
        if (route.getResponseHeaders() != null && !route.getResponseHeaders().isEmpty()) {
            try {
                Map<String, String> headerMap = OBJECT_MAPPER.readValue(route.getResponseHeaders(), Map.class);
                headerMap.forEach(headers::add);
            } catch (Exception e) {
                log.warn("Failed to parse response headers for route {}: {}", route.getId(), e.getMessage());
            }
        }

        if (headers.get(HttpHeaders.CONTENT_TYPE) == null && route.getContentType() != null) {
            headers.setContentType(MediaType.parseMediaType(route.getContentType()));
        }

        return ResponseEntity.status(route.getStatusCode())
                .headers(headers)
                .body(route.getResponseBody());
    }

    private void logRequest(HttpServletRequest request, String subPath, MockServer server,
                             ResponseEntity<String> responseEntity, boolean isChaosFailure, long startTime) {
        try {
            MockLog logEntry = new MockLog();
            logEntry.setMethod(request.getMethod());
            logEntry.setPath(subPath);
            logEntry.setStatusCode(responseEntity.getStatusCode().value());
            logEntry.setDurationMs(System.currentTimeMillis() - startTime);
            logEntry.setTimestamp(LocalDateTime.now());
            logEntry.setMockServer(server);
            logEntry.setResponseBody(responseEntity.getBody());
            logEntry.setChaosTriggered(isChaosFailure);
            mockLogRepository.save(logEntry);
        } catch (Exception e) {
            // Never fail a mock response just because logging failed.
            log.warn("Failed to persist mock log for server {}: {}", server.getId(), e.getMessage());
        }
    }
}
