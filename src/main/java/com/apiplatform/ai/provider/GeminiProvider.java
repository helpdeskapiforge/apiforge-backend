package com.apiplatform.ai.provider;

import com.apiplatform.ai.AIProvider;
import com.apiplatform.ai.AIProviderException;
import com.apiplatform.ai.AIResponse;
import com.apiplatform.config.AIProperties;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Fourth-priority (last-resort) provider: Google's Gemini API has a genuinely free
 * tier (generous daily request quota, no credit card) via a single {@code GEMINI_API_KEY} --
 * the easiest provider for someone with no local GPU and no OpenRouter account to get
 * running immediately, which is why it's kept as the guaranteed fallback rather than
 * first choice: local/free-model providers are preferred when available so nothing
 * leaves the user's machine and no quota is consumed unnecessarily.
 */
@Component
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final RestClient aiRestClient;
    private final AIProperties properties;

    public GeminiProvider(RestClient aiRestClient, AIProperties properties) {
        this.aiRestClient = aiRestClient;
        this.properties = properties;
    }

    @Override
    public AIResponse complete(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        String baseUrl = properties.getGemini().getBaseUrl();
        String apiKey = properties.getGemini().getApiKey();
        String model = getModel();

        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("GEMINI_API_KEY is not configured.");
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 4096
                )
        );

        try {
            JsonNode response = aiRestClient.post()
                    .uri(baseUrl + "/models/" + model + ":generateContent?key=" + apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode candidate = response == null ? null : response.path("candidates").path(0);
            JsonNode part = candidate == null ? null : candidate.path("content").path("parts").path(0);
            String content = part == null ? null : part.path("text").asText(null);

            if (content == null || content.isBlank()) {
                // A very common Gemini failure mode is finishReason=SAFETY/RECITATION with no text part.
                String finishReason = candidate == null ? "UNKNOWN" : candidate.path("finishReason").asText("UNKNOWN");
                throw new AIProviderException("Gemini returned no usable content (finishReason=" + finishReason + ").");
            }

            Integer tokens = response.path("usageMetadata").path("totalTokenCount").isMissingNode()
                    ? null : response.path("usageMetadata").path("totalTokenCount").asInt();
            long latency = System.currentTimeMillis() - start;
            return new AIResponse(content, getProviderName(), model, tokens, latency);
        } catch (RestClientException e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            throw new AIProviderException("Gemini request failed. Check GEMINI_API_KEY and that '" + model + "' is a valid model id.", e);
        }
    }

    @Override
    public boolean isAvailable() {
        String apiKey = properties.getGemini().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public String getModel() {
        return properties.getGemini().getModel();
    }
}