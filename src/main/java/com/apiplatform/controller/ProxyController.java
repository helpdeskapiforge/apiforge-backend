package com.apiplatform.controller;

import com.apiplatform.model.RequestHistory;
import com.apiplatform.model.User;
import com.apiplatform.payload.request.ProxyRequestDto;
import com.apiplatform.payload.response.ProxyResponseDto;
import com.apiplatform.repository.RequestHistoryRepository;
import com.apiplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    @Autowired
    RequestHistoryRepository historyRepository;
    @Autowired
    UserRepository userRepository; // To get current user ID

    @PostMapping("/execute")
    public ResponseEntity<?> executeRequest(@RequestBody ProxyRequestDto requestDto) {
        RestTemplate restTemplate = new RestTemplate();
        long startTime = System.currentTimeMillis();


        try {
            // 1. Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            if (requestDto.getHeaders() != null) {
                requestDto.getHeaders().forEach(headers::add);
            }

            // 2. Prepare Entity (Body + Headers)
            HttpEntity<String> entity = new HttpEntity<>(requestDto.getBody(), headers);

            // 3. Select Method
            HttpMethod method = HttpMethod.valueOf(requestDto.getMethod().toUpperCase());

            // 4. Execute Request
            ResponseEntity<String> response = restTemplate.exchange(
                    requestDto.getUrl(),
                    method,
                    entity,
                    String.class
            );

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // --- SAVE HISTORY ---
            try {
                // Get current user (Quick way for Phase 1)
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                User user = userRepository.findByEmail(auth.getName()).orElse(null);

                if (user != null) {
                    RequestHistory history = new RequestHistory();
                    history.setMethod(requestDto.getMethod());
                    history.setUrl(requestDto.getUrl());
                    history.setStatus(response.getStatusCode().value());
                    history.setDurationMs(duration);
                    history.setTimestamp(LocalDateTime.now());
                    history.setUserId(user.getId());

                    historyRepository.save(history);
                }
            } catch (Exception e) {
                // Don't fail the request if history save fails
                System.err.println("Failed to save history: " + e.getMessage());
            }

            // 5. Build Response
            return ResponseEntity.ok(new ProxyResponseDto(
                    response.getStatusCode().value(),
                    endTime - startTime,
                    convertHeaders(response.getHeaders()),
                    response.getBody()
            ));

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle 4xx and 5xx errors from the Target API nicely
            long endTime = System.currentTimeMillis();
            return ResponseEntity.ok(new ProxyResponseDto(
                    e.getStatusCode().value(),
                    endTime - startTime,
                    convertHeaders(e.getResponseHeaders()),
                    e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error executing request: " + e.getMessage());
        }
    }

    private Map<String, String> convertHeaders(HttpHeaders headers) {
        Map<String, String> map = new HashMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> map.put(k, v.get(0)));
        }
        return map;
    }
}