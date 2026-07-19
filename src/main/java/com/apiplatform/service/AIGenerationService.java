package com.apiplatform.service;

import com.apiplatform.ai.AIFeature;
import com.apiplatform.ai.AIProvider;
import com.apiplatform.ai.AIProviderException;
import com.apiplatform.ai.AIProviderResolver;
import com.apiplatform.ai.AIResponse;
import com.apiplatform.ai.prompt.CurlGeneratorPrompts;
import com.apiplatform.ai.prompt.ErrorLogExplainerPrompts;
import com.apiplatform.ai.prompt.JsonValidatorPrompts;
import com.apiplatform.ai.prompt.MockDataPrompts;
import com.apiplatform.ai.prompt.PostmanTestPrompts;
import com.apiplatform.ai.prompt.RegexGeneratorPrompts;
import com.apiplatform.ai.prompt.SqlGeneratorPrompts;
import com.apiplatform.ai.util.JsonStructureValidator;
import com.apiplatform.ai.util.MockDataHeuristicGenerator;
import com.apiplatform.ai.util.PostmanAssertionBuilder;
import com.apiplatform.ai.util.PromptSanitizer;
import com.apiplatform.exception.TooManyRequestsException;
import com.apiplatform.model.AIGeneration;
import com.apiplatform.model.User;
import com.apiplatform.repository.AIGenerationRepository;
import com.apiplatform.security.ratelimit.AIRateLimiter;
import com.apiplatform.web.dto.CurlGenerationRequest;
import com.apiplatform.web.dto.ErrorExplainRequest;
import com.apiplatform.web.dto.JsonValidateRequest;
import com.apiplatform.web.dto.MockDataRequest;
import com.apiplatform.web.dto.PostmanTestRequest;
import com.apiplatform.web.dto.RegexGenerationRequest;
import com.apiplatform.web.dto.SqlGenerationRequest;
import com.apiplatform.web.dto.response.AIGenerationResponse;
import com.apiplatform.web.dto.response.AIProviderStatusResponse;
import com.apiplatform.web.dto.response.JsonIssueResponse;
import com.apiplatform.web.dto.response.JsonValidationResponse;
import com.apiplatform.web.dto.response.PostmanTestResponse;
import com.apiplatform.web.mapper.ResponseMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AIGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AIGenerationService.class);
    private static final String NO_AI_PROVIDER = "deterministic"; // recorded as the "provider" when a request was served without calling any LLM

    private final AIProviderResolver providerResolver;
    private final AIGenerationRepository aiGenerationRepository;
    private final AIRateLimiter rateLimiter;
    private final PromptSanitizer promptSanitizer;
    private final JsonStructureValidator jsonStructureValidator;
    private final PostmanAssertionBuilder postmanAssertionBuilder;
    private final MockDataHeuristicGenerator mockDataHeuristicGenerator;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public AIGenerationService(AIProviderResolver providerResolver,
                               AIGenerationRepository aiGenerationRepository,
                               AIRateLimiter rateLimiter,
                               PromptSanitizer promptSanitizer,
                               JsonStructureValidator jsonStructureValidator,
                               PostmanAssertionBuilder postmanAssertionBuilder,
                               MockDataHeuristicGenerator mockDataHeuristicGenerator) {
        this.providerResolver = providerResolver;
        this.aiGenerationRepository = aiGenerationRepository;
        this.rateLimiter = rateLimiter;
        this.promptSanitizer = promptSanitizer;
        this.jsonStructureValidator = jsonStructureValidator;
        this.postmanAssertionBuilder = postmanAssertionBuilder;
        this.mockDataHeuristicGenerator = mockDataHeuristicGenerator;
    }

    // ---------------------------------------------------------------- cURL Generator

    public AIGenerationResponse generateCurl(User user, CurlGenerationRequest request) {
        String userPrompt = CurlGeneratorPrompts.buildUserPrompt(
                promptSanitizer, request.description(), request.baseUrl(), request.authHint());

        AIGeneration saved = callProviderAndPersist(user, AIFeature.CURL_GENERATOR, userPrompt,
                provider -> provider.complete(CurlGeneratorPrompts.SYSTEM, userPrompt));

        return ResponseMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------- Regex Generator

    public AIGenerationResponse generateRegex(User user, RegexGenerationRequest request) {
        String userPrompt = RegexGeneratorPrompts.buildUserPrompt(promptSanitizer, request.description());

        AIGeneration saved = callProviderAndPersist(user, AIFeature.REGEX_GENERATOR, userPrompt,
                provider -> provider.complete(RegexGeneratorPrompts.SYSTEM, userPrompt));

        return ResponseMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------- SQL Generator

    public AIGenerationResponse generateSql(User user, SqlGenerationRequest request) {
        String userPrompt = SqlGeneratorPrompts.buildUserPrompt(promptSanitizer, request.description(), request.dialect());

        AIGeneration saved = callProviderAndPersist(user, AIFeature.SQL_GENERATOR, userPrompt,
                provider -> provider.complete(SqlGeneratorPrompts.SYSTEM, userPrompt));

        return ResponseMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------- Error Log Explainer

    public AIGenerationResponse explainError(User user, ErrorExplainRequest request) {
        String userPrompt = ErrorLogExplainerPrompts.buildUserPrompt(promptSanitizer, request.logText(), request.context());

        AIGeneration saved = callProviderAndPersist(user, AIFeature.ERROR_LOG_EXPLAINER, userPrompt,
                provider -> provider.complete(ErrorLogExplainerPrompts.SYSTEM, userPrompt));

        return ResponseMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------- Postman Test Generator

    public PostmanTestResponse generatePostmanTests(User user, PostmanTestRequest request) {
        String deterministic = postmanAssertionBuilder.build(request.method(), request.statusCode(), request.responseBody());

        String userPrompt = PostmanTestPrompts.buildUserPrompt(
                promptSanitizer, request.method(), request.url(), request.statusCode(), request.responseBody(), deterministic);

        List<AIProvider> chain = providerResolver.resolveChain();
        String aiSuggestions = null;
        String providerName = NO_AI_PROVIDER;
        String model = null;

        if (!chain.isEmpty() && rateLimiter.tryAcquire(user.getId())) {
            for (AIProvider provider : chain) {
                try {
                    AIResponse response = provider.complete(PostmanTestPrompts.SYSTEM, userPrompt);
                    aiSuggestions = stripCodeFences(response.content());
                    providerName = response.providerName();
                    model = response.model();
                    persist(user, AIFeature.POSTMAN_TEST_GENERATOR, userPrompt, response, true, null);
                    break;
                } catch (AIProviderException e) {
                    log.warn("AI provider '{}' failed for Postman test enrichment, trying next: {}", provider.getProviderName(), e.getMessage());
                    persistFailure(user, AIFeature.POSTMAN_TEST_GENERATOR, userPrompt, provider, e);
                }
            }
        }

        String combined = aiSuggestions == null || aiSuggestions.isBlank()
                ? deterministic
                : deterministic + "\n// --- AI-suggested additional assertions ---\n" + aiSuggestions;

        return new PostmanTestResponse(null, providerName, model, deterministic, aiSuggestions, combined);
    }

    // ---------------------------------------------------------------- Mock Data Generator

    public AIGenerationResponse generateMockData(User user, MockDataRequest request) {
        String mode = request.mode() == null ? "SIMPLE" : request.mode().toUpperCase();
        int count = request.count() == null ? 10 : request.count();
        String userPrompt = MockDataPrompts.buildUserPrompt(promptSanitizer, request.description(), mode, count);

        Optional<AIProvider> provider = providerResolver.tryResolve();

        if (provider.isEmpty()) {
            // No AI configured -- fall back to the deterministic heuristic generator, but only if the
            // description already looks like a JSON shape (e.g. an example object). Free-text descriptions
            // genuinely need an AI provider to turn prose into a schema.
            JsonNode shape = tryParseJson(request.description());
            if (shape == null) {
                throw AIProviderException.noProviderAvailable();
            }
            String generated = mockDataHeuristicGenerator.generateFromShape(shape, count);
            AIGeneration record = new AIGeneration();
            record.setFeature(AIFeature.MOCK_DATA_GENERATOR);
            record.setProvider(NO_AI_PROVIDER);
            record.setModel("heuristic-faker");
            record.setPrompt(userPrompt);
            record.setResult(generated);
            record.setSuccess(true);
            record.setUser(user);
            AIGeneration saved = aiGenerationRepository.save(record);
            return ResponseMapper.toResponse(saved);
        }

        AIGeneration saved = callProviderAndPersist(user, AIFeature.MOCK_DATA_GENERATOR, userPrompt, ai -> {
            AIResponse response = ai.complete(MockDataPrompts.SYSTEM, userPrompt);
            String cleaned = stripCodeFences(response.content());
            JsonNode parsed = tryParseJson(cleaned);
            if (parsed == null) {
                // The model didn't return valid JSON. Repair rather than fail outright when possible.
                JsonNode shape = tryParseJson(request.description());
                if (shape != null) {
                    log.warn("Mock data provider returned invalid JSON; repairing with heuristic generator.");
                    String repaired = mockDataHeuristicGenerator.generateFromShape(shape, count);
                    return new AIResponse(repaired, response.providerName(), response.model(), response.tokensUsed(), response.latencyMs());
                }
                throw new AIProviderException("The AI provider returned invalid JSON and the description wasn't a JSON shape to repair from. Try rephrasing, or provide an example JSON object.");
            }
            return new AIResponse(cleaned, response.providerName(), response.model(), response.tokensUsed(), response.latencyMs());
        });

        return ResponseMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------- JSON Validator

    public JsonValidationResponse validateJson(User user, JsonValidateRequest request) {
        JsonStructureValidator.Result result = jsonStructureValidator.validate(request.json(), request.expectedSchema());

        List<JsonIssueResponse> issues = result.issues().stream()
                .map(i -> new JsonIssueResponse(i.path(), i.message()))
                .toList();

        if (result.isFullyValid()) {
            return new JsonValidationResponse(true, true, issues, null, null);
        }

        String explanation = null;
        String fixedJson = null;

        List<AIProvider> chain = providerResolver.resolveChain();
        if (!chain.isEmpty() && rateLimiter.tryAcquire(user.getId())) {
            String issuesText = issues.stream()
                    .map(i -> i.path() + ": " + i.message())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            String userPrompt = JsonValidatorPrompts.buildUserPrompt(promptSanitizer, request.json(), issuesText);

            for (AIProvider provider : chain) {
                try {
                    AIResponse response = provider.complete(JsonValidatorPrompts.SYSTEM, userPrompt);
                    persist(user, AIFeature.JSON_VALIDATOR, userPrompt, response, true, null);

                    String content = response.content();
                    int fixedIdx = content.indexOf("FIXED_JSON:");
                    if (fixedIdx >= 0) {
                        explanation = content.substring(0, fixedIdx).replace("EXPLANATION:", "").trim();
                        fixedJson = stripCodeFences(content.substring(fixedIdx + "FIXED_JSON:".length()).trim());
                    } else {
                        explanation = content.trim();
                    }
                    break;
                } catch (AIProviderException e) {
                    log.warn("AI provider '{}' failed for JSON validator explanation, trying next: {}", provider.getProviderName(), e.getMessage());
                    persistFailure(user, AIFeature.JSON_VALIDATOR, userPrompt, provider, e);
                }
            }
        }

        return new JsonValidationResponse(result.syntaxValid(), false, issues, explanation, fixedJson);
    }

    // ---------------------------------------------------------------- History & provider status

    @Transactional(readOnly = true)
    public Page<AIGeneration> getHistory(User user, Pageable pageable) {
        return aiGenerationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public AIProviderStatusResponse getProviderStatus() {
        List<AIProviderStatusResponse.Provider> providers = providerResolver.statusOfAll().stream()
                .map(s -> new AIProviderStatusResponse.Provider(s.name(), s.available(), s.model()))
                .toList();
        String active = providerResolver.tryResolve().map(AIProvider::getProviderName).orElse(null);
        return new AIProviderStatusResponse(providers, active);
    }

    // ---------------------------------------------------------------- shared helpers

    private AIGeneration callProviderAndPersist(User user, AIFeature feature, String userPrompt,
                                                java.util.function.Function<AIProvider, AIResponse> call) {
        List<AIProvider> chain = providerResolver.resolveChain();
        if (chain.isEmpty()) {
            throw AIProviderException.noProviderAvailable();
        }
        if (!rateLimiter.tryAcquire(user.getId())) {
            throw new TooManyRequestsException(
                    "AI request limit reached. Please wait a few minutes before trying again.");
        }

        StringBuilder attempts = new StringBuilder();
        for (int i = 0; i < chain.size(); i++) {
            AIProvider provider = chain.get(i);
            try {
                AIResponse response = call.apply(provider);
                return persist(user, feature, userPrompt, response, true, null);
            } catch (AIProviderException e) {
                persistFailure(user, feature, userPrompt, provider, e);
                attempts.append(attempts.isEmpty() ? "" : "; ").append(provider.getProviderName()).append(": ").append(e.getMessage());
                boolean hasNext = i < chain.size() - 1;
                log.warn("AI provider '{}' failed for feature={}{}: {}", provider.getProviderName(), feature,
                        hasNext ? " -- falling back to the next configured provider" : " -- no more providers to try", e.getMessage());
            }
        }
        throw new AIProviderException("Every configured AI provider failed. " + attempts +
                ". Check `docker compose logs api` (or your app logs) for details, and GET /api/v1/ai/providers/status for current reachability.");
    }

    private AIGeneration persist(User user, AIFeature feature, String prompt, AIResponse response, boolean success, String error) {
        AIGeneration record = new AIGeneration();
        record.setFeature(feature);
        record.setProvider(response.providerName());
        record.setModel(response.model());
        record.setPrompt(prompt);
        record.setResult(response.content());
        record.setTokensUsed(response.tokensUsed());
        record.setLatencyMs(response.latencyMs());
        record.setSuccess(success);
        record.setErrorMessage(error);
        record.setUser(user);
        log.info("AI generation feature={} provider={} model={} latencyMs={} tokens={} userId={}",
                feature, response.providerName(), response.model(), response.latencyMs(), response.tokensUsed(), user.getId());
        return aiGenerationRepository.save(record);
    }

    private void persistFailure(User user, AIFeature feature, String prompt, AIProvider provider, AIProviderException e) {
        AIGeneration record = new AIGeneration();
        record.setFeature(feature);
        record.setProvider(provider.getProviderName());
        record.setModel(provider.getModel());
        record.setPrompt(prompt);
        record.setSuccess(false);
        record.setErrorMessage(e.getMessage());
        record.setUser(user);
        log.warn("AI generation FAILED feature={} provider={} error={} userId={}", feature, provider.getProviderName(), e.getMessage(), user.getId());
        aiGenerationRepository.save(record);
    }

    private JsonNode tryParseJson(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return objectMapper.readTree(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Some providers ignore "no markdown fences" instructions; strip ```json ... ``` wrappers defensively. */
    private String stripCodeFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}