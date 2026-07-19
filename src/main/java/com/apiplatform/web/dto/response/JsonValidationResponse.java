package com.apiplatform.web.dto.response;

import java.util.List;

public record JsonValidationResponse(
        boolean syntaxValid,
        boolean structurallyValid,
        List<JsonIssueResponse> issues,
        /** Human-readable explanation, best-effort: null if no AI provider was available. */
        String explanation,
        /** AI-suggested corrected JSON, best-effort: null if no AI provider was available or syntax was already valid with no issues. */
        String suggestedFix
) {
}
