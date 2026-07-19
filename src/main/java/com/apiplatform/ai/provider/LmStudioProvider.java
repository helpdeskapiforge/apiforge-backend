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
 * Second-priority free provider: LM Studio (https://lmstudio.ai) exposes an
 * OpenAI-compatible chat-completions API on localhost once a model is loaded, so this
 * shares the same request/response shape as {@link OpenRouterProvider} minus
 * authentication -- no API key is sent or required for a local server.
 */
@Component
public class LmStudioProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(LmStudioProvider.class);

    private final RestClient aiRestClient;
    private final AIProperties properties;

    public LmStudioProvider(RestClient aiRestClient, AIProperties properties) {
        this.aiRestClient = aiRestClient;
        this.properties = properties;
    }

    @Override
    public AIResponse complete(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        String baseUrl = properties.getLmstudio().getBaseUrl();
        String model = getModel();

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
                    .uri(baseUrl + "/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode choice = response == null ? null : response.path("choices").path(0);
            String content = choice == null ? null : choice.path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new AIProviderException("LM Studio returned an empty response for model '" + model + "'.");
            }

            Integer tokens = response.path("usage").path("total_tokens").isMissingNode()
                    ? null : response.path("usage").path("total_tokens").asInt();
            long latency = System.currentTimeMillis() - start;
            return new AIResponse(content, getProviderName(), model, tokens, latency);
        } catch (RestClientException e) {
            log.warn("LM Studio call failed against {}: {}", baseUrl, e.getMessage());
            throw new AIProviderException("Could not reach LM Studio at " + baseUrl + ". Is a model loaded and the local server started?", e);
        }
    }

    @Override
    public boolean isAvailable() {
        String baseUrl = properties.getLmstudio().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) return false;
        try {
            JsonNode response = aiRestClient.get()
                    .uri(baseUrl + "/v1/models")
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.has("data");
        } catch (Exception e) {
            log.warn("LM Studio availability check failed against {}: {}", baseUrl, e.toString());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "lmstudio";
    }

    @Override
    public String getModel() {
        return properties.getLmstudio().getModel();
    }
}