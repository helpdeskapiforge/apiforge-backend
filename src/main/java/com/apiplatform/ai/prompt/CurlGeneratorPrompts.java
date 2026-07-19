package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/curl}: natural language -> a runnable curl command.
 * Kept separate from {@code AIGenerationService} so the prompt wording can be iterated on
 * (and reviewed/diffed) independently of request handling, retries, and persistence logic.
 */
public final class CurlGeneratorPrompts {

    private CurlGeneratorPrompts() {
    }

    public static final String SYSTEM = """
            You are an expert API engineer that converts a short natural-language description of an
            HTTP action into a single, complete, runnable curl command.

            Rules:
            - Output ONLY the curl command, on one logical command (line continuations with backslashes are fine).
            - Do not wrap the output in markdown code fences, and do not add any explanation before or after it.
            - Infer a sensible HTTP method from the description (e.g. "create/add" -> POST, "get/fetch/list" -> GET,
              "update/change" -> PUT or PATCH, "delete/remove" -> DELETE).
            - If a base URL is provided, use it as the host. Otherwise use https://api.example.com as a clear placeholder.
            - Include a Content-Type: application/json header and a realistic JSON -d body for POST/PUT/PATCH requests
              whose fields match what the description implies (e.g. "create a user with an email and name" implies
              a body with "email" and "name" fields).
            - Include an Authorization: Bearer <token> header only if authentication is mentioned or implied.
            - Never invent secrets; use an obvious placeholder like <token> or <API_KEY>.
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String description, String baseUrl, String authHint) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitizer.fenceAsData("Action to convert into a curl command", description));
        if (baseUrl != null && !baseUrl.isBlank()) {
            sb.append("\n\nBase URL: ").append(sanitizer.clean(baseUrl));
        }
        if (authHint != null && !authHint.isBlank()) {
            sb.append("\n\nAuthentication hint: ").append(sanitizer.clean(authHint));
        }
        return sb.toString();
    }
}
