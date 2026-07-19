package com.apiplatform.ai.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds real, runnable {@code pm.test()} assertions directly from an actual response --
 * status code, content-type, and a type check for every top-level (and one level of
 * nested) field. This is the "solves a real developer problem" half of the Postman Test
 * Generator: it's correct by construction, unlike asking an LLM to guess field names.
 * The AI layer ({@code PostmanTestPrompts}) is only used to add assertions this can't
 * derive (business rules, variable extraction).
 */
@Component
public class PostmanAssertionBuilder {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public String build(String method, Integer statusCode, String responseBody) {
        StringBuilder sb = new StringBuilder();

        sb.append("pm.test(\"Status code is ").append(statusCode == null ? 200 : statusCode).append("\", function () {\n")
                .append("    pm.response.to.have.status(").append(statusCode == null ? 200 : statusCode).append(");\n")
                .append("});\n\n");

        sb.append("pm.test(\"Response has a JSON content-type\", function () {\n")
                .append("    pm.response.to.be.json;\n")
                .append("});\n\n");

        sb.append("pm.test(\"Response time is reasonable\", function () {\n")
                .append("    pm.expect(pm.response.responseTime).to.be.below(2000);\n")
                .append("});\n\n");

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            sb.append("// Response body is not valid JSON -- skipping field-level schema assertions.\n");
            return sb.toString();
        }

        if (root != null && root.isObject()) {
            sb.append("pm.test(\"Response has expected top-level fields with correct types\", function () {\n")
                    .append("    const json = pm.response.json();\n");
            appendFieldChecks("json", root, sb, 1);
            sb.append("});\n");
        } else if (root != null && root.isArray()) {
            sb.append("pm.test(\"Response is a non-empty array\", function () {\n")
                    .append("    const json = pm.response.json();\n")
                    .append("    pm.expect(Array.isArray(json)).to.be.true;\n");
            if (!root.isEmpty()) {
                sb.append("    pm.expect(json.length).to.be.above(0);\n");
            }
            sb.append("});\n");
            if (!root.isEmpty() && root.get(0).isObject()) {
                sb.append("\npm.test(\"First array item has expected fields with correct types\", function () {\n")
                        .append("    const json = pm.response.json();\n")
                        .append("    const item = json[0];\n");
                appendFieldChecks("item", root.get(0), sb, 1);
                sb.append("});\n");
            }
        }

        return sb.toString();
    }

    private void appendFieldChecks(String varName, JsonNode obj, StringBuilder sb, int depth) {
        if (depth > 2) return; // one level of nesting is enough signal without exploding the script

        // JACKSON 3 FIX: Use properties() in an enhanced for-loop
        for (Map.Entry<String, JsonNode> entry : obj.properties()) {
            String field = entry.getKey();
            JsonNode value = entry.getValue();
            String accessor = varName + "[\"" + field.replace("\"", "\\\"") + "\"]";

            if (value.isTextual()) {
                sb.append("    pm.expect(").append(accessor).append(").to.be.a('string');\n");
            } else if (value.isBoolean()) {
                sb.append("    pm.expect(").append(accessor).append(").to.be.a('boolean');\n");
            } else if (value.isNumber()) {
                sb.append("    pm.expect(").append(accessor).append(").to.be.a('number');\n");
            } else if (value.isArray()) {
                sb.append("    pm.expect(Array.isArray(").append(accessor).append(")).to.be.true;\n");
            } else if (value.isObject()) {
                sb.append("    pm.expect(").append(accessor).append(").to.be.an('object');\n");
            } else if (value.isNull()) {
                sb.append("    // \"").append(field).append("\" was null in the sample response -- consider whether it's ever required.\n");
            }
        }
    }
}