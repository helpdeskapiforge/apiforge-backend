package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/mock-data}.
 */
public final class MockDataPrompts {

    private MockDataPrompts() {
    }

    public static final String SYSTEM = """
            You are a test-data generator. Given a description of a data shape and a generation mode,
            output ONLY a single valid JSON value (object or array) -- no markdown fences, no prose, no comments,
            no trailing commas. The output must be parseable by a strict JSON parser.

            Generation modes and what they mean:
            - SIMPLE: a flat object with only primitive fields (string/number/boolean), 4-8 fields.
            - NESTED: an object with at least one nested object and one array of objects, 2-3 levels deep.
            - LARGE: an array of 20-50 realistic objects matching the described shape.
            - EDGE_CASES: an array of 6-10 objects that each individually exercise a boundary condition
              (empty string, very long string, zero, negative number, null field, unicode/emoji, extra unexpected
              field, minimum/maximum plausible values) while still matching the described shape.
            - INVALID: an array of 4-8 objects that each individually VIOLATE the described shape in one clear way
              (wrong type for a field, missing a required-sounding field, malformed email/date/url string) -- for
              testing input validation, not for use as good data.
            - REALISTIC: a single object or small array (as implied by the description) using plausible,
              internally-consistent real-world values (real-looking names, valid-looking emails, coherent dates)
              rather than placeholder/lorem-ipsum text.

            Always infer reasonable field names and types from the description even if it's informal
            (e.g. "a user with an id, name and email" implies {"id": <int or uuid>, "name": <string>, "email": <string>}).
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String description, String mode, Integer count) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitizer.fenceAsData("Data shape description", description));
        sb.append("\n\nGeneration mode: ").append(mode == null ? "SIMPLE" : mode.toUpperCase());
        if (count != null && count > 0) {
            sb.append("\nRequested item count (if the mode produces an array): ").append(Math.min(count, 100));
        }
        return sb.toString();
    }
}
