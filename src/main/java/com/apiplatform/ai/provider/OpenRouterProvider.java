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
 * Third-priority provider: OpenRouter (https://openrouter.ai) proxies many models,
 * including a rotating set of {@code :free}-suffixed ones that cost nothing but do
 * require a free account + API key. Chosen over paid providers by default via
 * {@code ai.openrouter.model=meta-llama/llama-3.1-8b-instruct:free}.
 */
@Component
public class OpenRouterProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterProvider.class);

    private final RestClient aiRestClient;
    private final AIProperties properties;

    public OpenRouterProvider(RestClient aiRestClient, AIProperties properties) {
        this.aiRestClient = aiRestClient;
        this.properties = properties;
    }

    @Override
    public AIResponse complete(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        String baseUrl = properties.getOpenrouter().getBaseUrl();
        String apiKey = properties.getOpenrouter().getApiKey();
        String model = getModel();

        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("OPENROUTER_API_KEY is not configured.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        try {
            JsonNode response = aiRestClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    // Required by OpenRouter's usage policy for free-tier attribution; harmless otherwise.
                    .header("HTTP-Referer", "https://github.com/helpdeskapiforge/apiforge-backend")
                    .header("X-Title", "API Forge")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode choice = response == null ? null : response.path("choices").path(0);
            String content = choice == null ? null : choice.path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new AIProviderException("OpenRouter returned an empty response for model '" + model + "'.");
            }

            Integer tokens = response.path("usage").path("total_tokens").isMissingNode()
                    ? null : response.path("usage").path("total_tokens").asInt();
            long latency = System.currentTimeMillis() - start;
            return new AIResponse(content, getProviderName(), model, tokens, latency);
        } catch (RestClientException e) {
            log.warn("OpenRouter call failed: {}", e.getMessage());
            throw new AIProviderException("OpenRouter request failed. Check OPENROUTER_API_KEY and rate limits.", e);
        }
    }

    @Override
    public boolean isAvailable() {
        String apiKey = properties.getOpenrouter().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "openrouter";
    }

    @Override
    public String getModel() {
        return properties.getOpenrouter().getModel();
    }
}