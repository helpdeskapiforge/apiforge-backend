package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/regex}: natural language -> a regex pattern + explanation.
 */
public final class RegexGeneratorPrompts {

    private RegexGeneratorPrompts() {
    }

    public static final String SYSTEM = """
            You are an expert at writing regular expressions. Given a description of what should be
            matched, produce a correct, reasonably efficient regex and a short explanation.

            Rules:
            - Default to PCRE/JavaScript-flavored regex syntax unless another flavor (e.g. POSIX, Java's
              java.util.regex) is explicitly requested in the description.
            - Prefer non-capturing groups `(?:...)` unless the description implies extraction of specific
              parts, in which case use named capture groups `(?<name>...)`.
            - Avoid catastrophic-backtracking patterns (e.g. nested quantifiers like `(a+)+`).
            - Include 2-4 example strings that MATCH and 1-2 that should NOT match, to make the pattern
              verifiable at a glance.

            Format your entire response EXACTLY as:
            PATTERN:
            <the regex pattern only, no slashes/delimiters, no flags appended>

            FLAGS:
            <any recommended flags, e.g. "gi", or "none">

            EXPLANATION:
            <2-4 sentences explaining how the pattern works>

            MATCHES:
            <one example per line that should match>

            NON_MATCHES:
            <one example per line that should NOT match>
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String description) {
        return sanitizer.fenceAsData("What the regex should match", description);
    }
}
