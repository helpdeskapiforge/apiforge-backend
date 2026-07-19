package com.apiplatform.ai;

/**
 * Thrown when no AI provider is configured/reachable, or the configured provider
 * returns an error. Mapped to HTTP 502 by {@link com.apiplatform.exception.GlobalExceptionHandler}
 * -- distinct from a 500, because the failure is in an upstream dependency, not in our
 * own code, and the client (frontend) needs to tell those two apart to show a useful
 * "configure an AI provider" message instead of a generic error.
 */
public class AIProviderException extends RuntimeException {

    public AIProviderException(String message) {
        super(message);
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public static AIProviderException noProviderAvailable() {
        return new AIProviderException(
                "No AI provider is currently available. Configure one of OLLAMA_BASE_URL, " +
                        "LM_STUDIO_BASE_URL, OPENROUTER_API_KEY, or GEMINI_API_KEY. See README.md > AI Providers."
        );
    }
}
