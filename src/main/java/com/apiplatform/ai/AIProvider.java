package com.apiplatform.ai;

/**
 * A pluggable large-language-model backend.
 * <p>
 * Every AI feature in the application (cURL generation, mock data, etc.) talks to
 * this interface only -- never to a concrete HTTP client for Ollama/OpenRouter/Gemini
 * directly. That is what lets {@link AIProviderResolver} swap providers based on
 * what's actually reachable/configured without a single line of feature code changing,
 * and what lets a future provider be added by writing one new class and nothing else.
 */
public interface AIProvider {

    /**
     * Send a single-turn completion request.
     *
     * @param systemPrompt fixed instructions describing the task (comes from a
     *                      dedicated prompt-template class, never inlined in a service)
     * @param userPrompt    the user/task-specific content
     * @return the model's response plus metadata about the call
     * @throws AIProviderException if the provider is unreachable, misconfigured, or
     *                              returns an error/empty response
     */
    AIResponse complete(String systemPrompt, String userPrompt);

    /**
     * Cheap reachability/configuration check, used by {@link AIProviderResolver} to pick
     * the first usable provider and by the frontend's provider-status banner. Must not
     * throw -- return {@code false} on any failure.
     */
    boolean isAvailable();

    /**
     * Stable machine-readable id, e.g. {@code "ollama"}, {@code "openrouter"}, {@code "gemini"}.
     * Persisted alongside every {@code AIGeneration} row for auditability.
     */
    String getProviderName();

    /**
     * The concrete model in use, e.g. {@code "llama3.1"} or {@code "gemini-2.5-flash"}.
     */
    String getModel();
}
