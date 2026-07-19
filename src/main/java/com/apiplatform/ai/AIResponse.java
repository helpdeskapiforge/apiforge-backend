package com.apiplatform.ai;

/**
 * Result of a single {@link AIProvider#complete(String, String)} call.
 *
 * @param content      the raw text completion
 * @param providerName which provider served this, e.g. "gemini"
 * @param model        the concrete model used, e.g. "gemini-2.5-flash"
 * @param tokensUsed   total tokens reported by the provider, or {@code null} if the
 *                     provider doesn't report usage (e.g. Ollama's native API)
 * @param latencyMs    wall-clock time for the HTTP round trip
 */
public record AIResponse(
        String content,
        String providerName,
        String model,
        Integer tokensUsed,
        long latencyMs
) {
}
