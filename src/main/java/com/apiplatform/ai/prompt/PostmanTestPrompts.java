package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/postman-tests}.
 * <p>
 * Note: the base assertions (status code, content-type, per-field schema/type checks,
 * response-time budget) are generated deterministically by
 * {@code AIGenerationService#buildDeterministicAssertions} directly from the actual
 * response JSON -- that's more reliable than asking an LLM to "guess" field names and
 * types. The AI is only asked to add higher-value assertions a static analysis can't
 * produce: business-rule checks, token/variable extraction, and edge cases implied by
 * the endpoint's intent.
 */
public final class PostmanTestPrompts {

    private PostmanTestPrompts() {
    }

    public static final String SYSTEM = """
            You are a senior QA/SDET writing Postman (pm.test) test scripts for a REST API response.

            You will be given the HTTP method, URL, status code, and response body of an actual API call,
            plus a set of assertions that were ALREADY generated deterministically (status code, content-type,
            per-field type checks). Do not repeat those.

            Add ONLY additional, genuinely useful pm.test() blocks such as:
            - extracting an id/token/value from the response into a Postman environment variable via
              pm.environment.set(...), when the response looks like it contains one (e.g. an auth token, a
              created resource id)
            - sensible business-rule assertions implied by the field names and values (e.g. an "email" field
              should look like an email, a "createdAt" should be a valid ISO date, a "price" should be >= 0)
            - a response-time assertion (pm.expect(pm.response.responseTime).to.be.below(...))
            - edge-case checks worth calling out as TODO comments if you cannot verify them from the response alone

            Output ONLY valid JavaScript (the body of a Postman test script -- one or more pm.test(...) calls).
            No markdown fences, no prose before or after.
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String method, String url, Integer statusCode,
                                          String responseBody, String deterministicAssertions) {
        return "Method: " + sanitizer.clean(method) +
                "\nURL: " + sanitizer.clean(url) +
                "\nStatus code: " + statusCode +
                "\n\n" + sanitizer.fenceAsData("Response body", responseBody) +
                "\n\n" + sanitizer.fenceAsData("Assertions already generated (do not repeat these)", deterministicAssertions);
    }
}
