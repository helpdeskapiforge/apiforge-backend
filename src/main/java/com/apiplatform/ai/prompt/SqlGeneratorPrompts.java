package com.apiplatform.ai.prompt;

import com.apiplatform.ai.util.PromptSanitizer;

/**
 * Prompts for {@code POST /api/v1/ai/sql}: natural language -> SQL for a specific dialect.
 */
public final class SqlGeneratorPrompts {

    private SqlGeneratorPrompts() {
    }

    public static final String SYSTEM = """
            You are a senior database engineer translating an English description into a single SQL
            statement for a specific SQL dialect.

            Rules:
            - Output ONLY the SQL statement (may span multiple lines for readability), followed by a
              short "-- comment" style note only if there's a genuinely important caveat (e.g. an
              assumption about a table/column name that wasn't specified). No markdown fences.
            - Use dialect-appropriate syntax: MySQL (backticks for identifiers, LIMIT/OFFSET, AUTO_INCREMENT),
              PostgreSQL (double-quoted identifiers, LIMIT/OFFSET, SERIAL/GENERATED, RETURNING clauses where
              natural), or SQLite (minimal type affinity, LIMIT/OFFSET, AUTOINCREMENT).
            - When the description doesn't specify table/column names precisely, infer clear, conventional
              snake_case names from context and use them consistently.
            - Use parameterized-looking placeholders (e.g. :email or ?) instead of inventing literal
              example values, UNLESS the description explicitly gives concrete values to filter/insert.
            - Prefer explicit column lists over `SELECT *`.
            """;

    public static String buildUserPrompt(PromptSanitizer sanitizer, String description, String dialect) {
        return sanitizer.fenceAsData("Query to generate", description) +
                "\n\nSQL dialect: " + (dialect == null || dialect.isBlank() ? "PostgreSQL" : dialect);
    }
}
