package com.apiplatform.controller;

import com.apiplatform.model.AIGeneration;
import com.apiplatform.model.User;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.AIGenerationService;
import com.apiplatform.web.dto.CurlGenerationRequest;
import com.apiplatform.web.dto.ErrorExplainRequest;
import com.apiplatform.web.dto.JsonValidateRequest;
import com.apiplatform.web.dto.MockDataRequest;
import com.apiplatform.web.dto.PageResponse;
import com.apiplatform.web.dto.PostmanTestRequest;
import com.apiplatform.web.dto.RegexGenerationRequest;
import com.apiplatform.web.dto.SqlGenerationRequest;
import com.apiplatform.web.dto.response.AIGenerationResponse;
import com.apiplatform.web.dto.response.AIProviderStatusResponse;
import com.apiplatform.web.dto.response.JsonValidationResponse;
import com.apiplatform.web.dto.response.PostmanTestResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI Tools: every endpoint here is authenticated by the same global JWT rule as the
 * rest of the API (see WebSecurityConfig -- {@code anyRequest().authenticated()} already
 * covers {@code /api/v1/ai/**}, no security config changes were needed to add this).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private static final int MAX_PAGE_SIZE = 100;

    private final AIGenerationService aiGenerationService;
    private final CurrentUserProvider currentUserProvider;

    public AIController(AIGenerationService aiGenerationService, CurrentUserProvider currentUserProvider) {
        this.aiGenerationService = aiGenerationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/curl")
    public ResponseEntity<AIGenerationResponse> generateCurl(@Valid @RequestBody CurlGenerationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.generateCurl(user, request));
    }

    @PostMapping("/regex")
    public ResponseEntity<AIGenerationResponse> generateRegex(@Valid @RequestBody RegexGenerationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.generateRegex(user, request));
    }

    @PostMapping("/sql")
    public ResponseEntity<AIGenerationResponse> generateSql(@Valid @RequestBody SqlGenerationRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.generateSql(user, request));
    }

    @PostMapping("/explain-error")
    public ResponseEntity<AIGenerationResponse> explainError(@Valid @RequestBody ErrorExplainRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.explainError(user, request));
    }

    @PostMapping("/postman-tests")
    public ResponseEntity<PostmanTestResponse> generatePostmanTests(@Valid @RequestBody PostmanTestRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.generatePostmanTests(user, request));
    }

    @PostMapping("/mock-data")
    public ResponseEntity<AIGenerationResponse> generateMockData(@Valid @RequestBody MockDataRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.generateMockData(user, request));
    }

    @PostMapping("/json-validate")
    public ResponseEntity<JsonValidationResponse> validateJson(@Valid @RequestBody JsonValidateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(aiGenerationService.validateJson(user, request));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<AIGenerationResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        User user = currentUserProvider.getCurrentUser();
        int clampedSize = size <= 0 ? 30 : Math.min(size, MAX_PAGE_SIZE);
        Page<AIGeneration> result = aiGenerationService.getHistory(user, PageRequest.of(Math.max(page, 0), clampedSize));
        PageResponse<AIGeneration> paged = PageResponse.from(result);
        PageResponse<AIGenerationResponse> response = new PageResponse<>(
                paged.data().stream().map(ResponseMapper::toResponse).toList(),
                paged.page(), paged.size(), paged.totalElements(), paged.totalPages(), paged.hasNext());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/status")
    public ResponseEntity<AIProviderStatusResponse> getProviderStatus() {
        return ResponseEntity.ok(aiGenerationService.getProviderStatus());
    }
}
