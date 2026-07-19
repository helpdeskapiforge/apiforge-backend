package com.apiplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Type-safe binding for the {@code ai.*} block in application.yml, instead of scattering
 * {@code @Value("${ai...}")} across four provider classes. Every provider reads its own
 * settings from here; nothing under {@code com.apiplatform.ai} touches environment
 * variables directly.
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    /** Resolution order, e.g. ["ollama", "lmstudio", "openrouter", "gemini"]. */
    private List<String> providerPriority = List.of("ollama", "lmstudio", "openrouter", "gemini");

    private int requestTimeoutSeconds = 90;
    private int maxPromptChars = 12000;

    private final Ollama ollama = new Ollama();
    private final LmStudio lmstudio = new LmStudio();
    private final OpenRouter openrouter = new OpenRouter();
    private final Gemini gemini = new Gemini();
    private final RateLimit rateLimit = new RateLimit();

    public List<String> getProviderPriority() {
        return providerPriority;
    }

    public void setProviderPriority(List<String> providerPriority) {
        this.providerPriority = providerPriority;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public LmStudio getLmstudio() {
        return lmstudio;
    }

    public OpenRouter getOpenrouter() {
        return openrouter;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class Ollama {
        private String baseUrl;
        private String model;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class LmStudio {
        private String baseUrl;
        private String model;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class OpenRouter {
        private String baseUrl;
        private String apiKey;
        private String model;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Gemini {
        private String baseUrl;
        private String apiKey;
        private String model;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class RateLimit {
        private int maxRequests = 20;
        private int windowSeconds = 300;

        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    }
}
