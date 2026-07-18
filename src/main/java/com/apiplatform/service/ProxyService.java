package com.apiplatform.service;

import com.apiplatform.model.RequestHistory;
import com.apiplatform.model.User;
import com.apiplatform.payload.request.ProxyRequestDto;
import com.apiplatform.payload.response.ProxyResponseDto;
import com.apiplatform.repository.RequestHistoryRepository;
import com.apiplatform.security.ssrf.UrlSafetyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);
    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    private final RestTemplate restTemplate;
    private final RequestHistoryRepository historyRepository;
    private final UrlSafetyValidator urlSafetyValidator;

    public ProxyService(RestTemplate proxyRestTemplate,
                         RequestHistoryRepository historyRepository,
                         UrlSafetyValidator urlSafetyValidator) {
        this.restTemplate = proxyRestTemplate;
        this.historyRepository = historyRepository;
        this.urlSafetyValidator = urlSafetyValidator;
    }

    public ProxyResponseDto execute(ProxyRequestDto requestDto, User currentUser) {
        if (requestDto.getUrl() == null || requestDto.getUrl().isBlank()) {
            throw new IllegalArgumentException("url is required.");
        }
        if (requestDto.getMethod() == null || !ALLOWED_METHODS.contains(requestDto.getMethod().toUpperCase())) {
            throw new IllegalArgumentException("method must be one of " + ALLOWED_METHODS);
        }

        urlSafetyValidator.assertSafe(requestDto.getUrl());

        HttpHeaders headers = new HttpHeaders();
        if (requestDto.getHeaders() != null) {
            requestDto.getHeaders().forEach(headers::add);
        }
        HttpEntity<String> entity = new HttpEntity<>(requestDto.getBody(), headers);
        HttpMethod method = HttpMethod.valueOf(requestDto.getMethod().toUpperCase());

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestDto.getUrl(), method, entity, String.class);
            long duration = System.currentTimeMillis() - startTime;

            recordHistory(requestDto, response.getStatusCode().value(), duration, currentUser);

            return new ProxyResponseDto(
                    response.getStatusCode().value(),
                    duration,
                    convertHeaders(response.getHeaders()),
                    response.getBody());
        } catch (HttpStatusCodeException e) {
            long duration = System.currentTimeMillis() - startTime;
            recordHistory(requestDto, e.getStatusCode().value(), duration, currentUser);
            return new ProxyResponseDto(
                    e.getStatusCode().value(),
                    duration,
                    convertHeaders(e.getResponseHeaders()),
                    e.getResponseBodyAsString());
        }
    }

    private void recordHistory(ProxyRequestDto requestDto, int status, long duration, User user) {
        try {
            RequestHistory history = new RequestHistory();
            history.setMethod(requestDto.getMethod());
            history.setUrl(requestDto.getUrl());
            history.setStatus(status);
            history.setDurationMs(duration);
            history.setTimestamp(LocalDateTime.now());
            history.setUserId(user.getId());
            historyRepository.save(history);
        } catch (Exception e) {
            // Never fail the user's request just because audit logging failed.
            log.warn("Failed to persist request history for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private Map<String, String> convertHeaders(HttpHeaders headers) {
        Map<String, String> map = new HashMap<>();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (!v.isEmpty()) {
                    map.put(k, v.get(0));
                }
            });
        }
        return map;
    }
}
