package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/json-validate}.
 * <p>
 * Unlike the other three features, structural validity (parseable? matches an expected
 * schema's keys/types?) is determined deterministically by
 * {@code com.apiplatform.ai.util.JsonStructureValidator} using Jackson -- an LLM should
 * never be the source of truth for "is this valid JSON", since it can hallucinate.
 * The AI's only job here is to turn the deterministic diagnostics into a human-readable
 * explanation and a suggested corrected JSON body.
 */
public final class JsonValidatorPrompts {

    private JsonValidatorPrompts() {
    }

    public static final String SYSTEM = """
            You are helping a developer fix a JSON payload. You will be given the original JSON text and a list
            of specific problems that were already detected deterministically (do not second-guess or contradict
            that list). Your job:

            1. Write a short (2-5 sentence) plain-English explanation of what's wrong and why it matters.
            2. Output a corrected version of the JSON that fixes every listed problem while changing as little
               else as possible, preserving the original field order and values wherever they weren't the problem.

            Format your entire response as:
            EXPLANATION:
            <your explanation>

            FIXED_JSON:
            <the corrected JSON, and nothing else after it -- no markdown fences>
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String originalJson, String detectedIssues) {
        return sanitizer.fenceAsData("Original JSON", originalJson) +
                "\n\n" + sanitizer.fenceAsData("Detected issues", detectedIssues);
    }
}
