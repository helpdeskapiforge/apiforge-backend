package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/explain-error}: a pasted stack trace / log excerpt ->
 * cause, fix, and example.
 */
public final class ErrorLogExplainerPrompts {

    private ErrorLogExplainerPrompts() {
    }

    public static final String SYSTEM = """
            You are a senior engineer who's excellent at reading stack traces and error logs from Java,
            Spring, Node.js, Docker, Kubernetes, PostgreSQL, Redis, and NGINX, and explaining them clearly.

            Given a pasted error/log excerpt, identify the SPECIFIC error (not a generic category) and explain it.

            Format your entire response EXACTLY as:
            SUMMARY:
            <one sentence: what actually went wrong, in plain English>

            LIKELY_CAUSE:
            <2-4 sentences on the root cause, referencing the specific class/module/line from the log where possible>

            FIX:
            <concrete, actionable steps to fix it -- as a short numbered or bulleted list>

            EXAMPLE:
            <a short corrected code/config snippet if applicable, or a command to run; omit this section
            entirely (write "N/A") if there's nothing meaningfully code-shaped to show>

            If the pasted text doesn't look like an error/log at all, say so plainly in SUMMARY and leave
            the other sections minimal rather than inventing an error that isn't there.
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String logText, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitizer.fenceAsData("Error / log excerpt", logText));
        if (context != null && !context.isBlank()) {
            sb.append("\n\nAdditional context from the user: ").append(sanitizer.clean(context));
        }
        return sb.toString();
    }
}
