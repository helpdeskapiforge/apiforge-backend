package com.apiplatform.controller;

import com.apiplatform.model.MockLog;
import com.apiplatform.model.MockRoute;
import com.apiplatform.model.MockServer;
import com.apiplatform.repository.MockLogRepository;
import com.apiplatform.repository.MockRouteRepository;
import com.apiplatform.repository.MockServerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/mock/simulator")
public class MockSimulatorController {

    @Autowired MockServerRepository mockServerRepository;
    @Autowired MockRouteRepository mockRouteRepository;
    @Autowired MockLogRepository mockLogRepository;

    @RequestMapping(value = "/{prefix}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> handleMockRequest(@PathVariable String prefix, HttpServletRequest request) throws InterruptedException {

        long startTime = System.currentTimeMillis();

        // 1. Find Server
        MockServer server = mockServerRepository.findByPathPrefix(prefix)
                .orElseThrow(() -> new RuntimeException("Mock Server not found"));

        // 2. Extract Path
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String subPath = fullPath.replace("/api/mock/simulator/" + prefix, "");
        if (subPath.isEmpty()) subPath = "/";

        // 3. Find Route (Repo query now checks isEnabled=true)
        MockRoute route = mockRouteRepository.findMatchingRoute(server.getId(), request.getMethod(), subPath)
                .orElse(null);

        if (route == null) {
            return ResponseEntity.status(404).body("No active mock route found for: " + request.getMethod() + " " + subPath);
        }

        // 4. Delay Simulation
        if (route.getDelayMs() > 0) {
            Thread.sleep(route.getDelayMs());
        }

        // 5. Chaos Monkey
        boolean isChaosFailure = false;
        if (route.isChaosEnabled()) {
            isChaosFailure = new Random().nextDouble() < route.getFailureRate();
        }

        ResponseEntity<String> responseEntity;

        if (isChaosFailure) {
            responseEntity = ResponseEntity.status(500).body("Chaos Monkey: Failure Simulated");
        } else {
            // --- NEW: Header Parsing Logic ---
            HttpHeaders headers = new HttpHeaders();
            if (route.getResponseHeaders() != null && !route.getResponseHeaders().isEmpty()) {
                try {
                    // Parse JSON string {"Key": "Value"} into Map
                    Map<String, String> headerMap = new ObjectMapper().readValue(route.getResponseHeaders(), Map.class);
                    headerMap.forEach(headers::add);
                } catch (Exception e) {
                    System.err.println("Failed to parse mock headers: " + e.getMessage());
                }
            }

            // Allow Content-Type override if not in headers
            if (headers.get(HttpHeaders.CONTENT_TYPE) == null && route.getContentType() != null) {
                headers.setContentType(MediaType.parseMediaType(route.getContentType()));
            }


            responseEntity = ResponseEntity.status(route.getStatusCode())
                    .headers(headers)
                    .body(route.getResponseBody());
        }

        // 6. Logging
        long duration = System.currentTimeMillis() - startTime;
        MockLog log = new MockLog();
        log.setMethod(request.getMethod());
        log.setPath(subPath);
        log.setStatusCode(responseEntity.getStatusCode().value());
        log.setDurationMs(duration);
        log.setTimestamp(LocalDateTime.now());
        log.setMockServer(server);
        log.setResponseBody(responseEntity.getBody());
        log.setChaosTriggered(isChaosFailure);
        mockLogRepository.save(log);

        return responseEntity;
    }
}