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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Highest-priority provider by default: a local Ollama install (https://ollama.com) is
 * completely free, keeps prompts off the network entirely, and needs zero API key
 * configuration -- ideal default for local development and for anyone evaluating this
 * project without wanting to hand out a Gemini/OpenRouter key.
 * <p>
 * Uses Ollama's native {@code /api/generate} endpoint (not the OpenAI-compatible one)
 * since it's simpler and gives us {@code eval_count} for free token accounting.
 */
@Component
public class OllamaProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    private final RestClient aiRestClient;
    private final AIProperties properties;

    public OllamaProvider(RestClient aiRestClient, AIProperties properties) {
        this.aiRestClient = aiRestClient;
        this.properties = properties;
    }

    @Override
    public AIResponse complete(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        String baseUrl = properties.getOllama().getBaseUrl();
        String model = getModel();

        Map<String, Object> body = Map.of(
                "model", model,
                "system", systemPrompt,
                "prompt", userPrompt,
                "stream", false
        );

        try {
            JsonNode response = aiRestClient.post()
                    .uri(baseUrl + "/api/generate")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.hasNonNull("response")) {
                throw new AIProviderException("Ollama returned an empty response for model '" + model + "'.");
            }

            String content = response.get("response").asText();
            Integer tokens = response.hasNonNull("eval_count") ? response.get("eval_count").asInt() : null;
            long latency = System.currentTimeMillis() - start;
            return new AIResponse(content, getProviderName(), model, tokens, latency);
        } catch (RestClientException e) {
            log.warn("Ollama call failed against {}: {}", baseUrl, e.getMessage());
            throw new AIProviderException("Could not reach Ollama at " + baseUrl + ". Is it running?", e);
        }
    }

    @Override
    public boolean isAvailable() {
        String baseUrl = properties.getOllama().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) return false;
        try {
            JsonNode response = aiRestClient.get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.has("models");
        } catch (Exception e) {
            log.warn("Ollama availability check failed against {}: {}", baseUrl, e.toString());
            return false;
        }
    }

    /** Lists locally pulled model names (e.g. "llama3:latest"), for a model picker in the UI. */
    public List<String> listInstalledModels() {
        String baseUrl = properties.getOllama().getBaseUrl();
        try {
            JsonNode response = aiRestClient.get()
                    .uri(baseUrl + "/api/tags")
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) return List.of();

            JsonNode modelsNode = response.get("models");
            if (modelsNode == null || !modelsNode.isArray()) return List.of();

            // JACKSON 3 FIX: Explicitly iterate the array instead of using findValuesAsText()
            List<String> modelNames = new ArrayList<>();
            for (JsonNode modelNode : modelsNode) {
                JsonNode nameNode = modelNode.get("name");
                if (nameNode != null && !nameNode.isNull()) {
                    modelNames.add(nameNode.asText());
                }
            }
            return modelNames;

        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public String getModel() {
        return properties.getOllama().getModel();
    }
}