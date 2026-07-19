package com.apiplatform.ai.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic JSON diagnostics: syntax validity and, when an "expected shape" JSON is
 * supplied, a structural diff (missing fields, unexpected extra fields, type mismatches).
 * <p>
 * Deliberately does not use an LLM: whether a document is syntactically valid JSON, or
 * whether a field is a string where an example says it should be a number, is a fact
 * computable exactly -- asking a model to "judge" that would be strictly worse (slower,
 * costs a request, and can be wrong) than parsing it. The AI layer
 * ({@code JsonValidatorPrompts}) only turns these findings into a human explanation +
 * suggested fix.
 */
@Component
public class JsonStructureValidator {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public record Issue(String path, String message) {
    }

    public record Result(boolean syntaxValid, JsonNode parsed, List<Issue> issues) {
        public boolean isFullyValid() {
            return syntaxValid && issues.isEmpty();
        }
    }

    public Result validate(String json, String expectedSchemaJson) {
        List<Issue> issues = new ArrayList<>();
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(json);
            if (parsed == null) {
                issues.add(new Issue("$", "Input is empty."));
                return new Result(false, null, issues);
            }
        } catch (RuntimeException e) {
            // Jackson 3's JacksonException (the base for parse/syntax errors) is unchecked, unlike
            // Jackson 2's JsonProcessingException. Line/column info is already embedded in the message
            // text by Jackson itself, so there's no need to depend on JsonLocation's exact API shape.
            issues.add(new Issue("$", "Invalid JSON syntax: " + firstLine(e.getMessage())));
            return new Result(false, null, issues);
        }

        if (expectedSchemaJson != null && !expectedSchemaJson.isBlank()) {
            try {
                JsonNode expected = objectMapper.readTree(expectedSchemaJson);
                diff("$", expected, parsed, issues);
            } catch (RuntimeException e) {
                issues.add(new Issue("$", "Expected-shape JSON could not itself be parsed, skipping structural comparison."));
            }
        }

        return new Result(true, parsed, issues);
    }

    /** Recursively compares an "expected shape" node against the actual node. */
    private void diff(String path, JsonNode expected, JsonNode actual, List<Issue> issues) {
        if (expected.isObject() && actual != null && actual.isObject()) {
            ObjectNode expectedObj = (ObjectNode) expected;
            ObjectNode actualObj = (ObjectNode) actual;

            // JACKSON 3 FIX: Use properties() instead of fields()
            for (Map.Entry<String, JsonNode> entry : expectedObj.properties()) {
                String field = entry.getKey();
                String childPath = path + "." + field;
                if (!actualObj.has(field)) {
                    issues.add(new Issue(childPath, "Missing expected field \"" + field + "\"."));
                } else {
                    JsonNode actualChild = actualObj.get(field);
                    if (actualChild.isNull() && !entry.getValue().isNull()) {
                        issues.add(new Issue(childPath, "Field \"" + field + "\" is null but expected a " + typeName(entry.getValue()) + "."));
                    } else if (!typesCompatible(entry.getValue(), actualChild)) {
                        issues.add(new Issue(childPath, "Field \"" + field + "\" has type " + typeName(actualChild) +
                                " but expected " + typeName(entry.getValue()) + "."));
                    } else if (entry.getValue().isObject() || entry.getValue().isArray()) {
                        diff(childPath, entry.getValue(), actualChild, issues);
                    }
                }
            }

            // JACKSON 3 FIX: Use propertyNames() instead of fieldNames()
            for (String field : actualObj.propertyNames()) {
                if (!expectedObj.has(field)) {
                    issues.add(new Issue(path + "." + field, "Unexpected field \"" + field + "\" not present in the expected shape."));
                }
            }
        } else if (expected.isArray() && actual != null && actual.isArray()) {
            ArrayNode expectedArr = (ArrayNode) expected;
            ArrayNode actualArr = (ArrayNode) actual;
            if (actualArr.isEmpty() || expectedArr.isEmpty()) return;
            // Use the first element of the expected array as the element schema, and check every actual element.
            JsonNode elementSchema = expectedArr.get(0);
            for (int i = 0; i < actualArr.size(); i++) {
                diff(path + "[" + i + "]", elementSchema, actualArr.get(i), issues);
            }
        } else if (!typesCompatible(expected, actual)) {
            issues.add(new Issue(path, "Expected " + typeName(expected) + " but found " + typeName(actual) + "."));
        }
    }

    private boolean typesCompatible(JsonNode expected, JsonNode actual) {
        if (actual == null) return false;
        if (expected.isNumber()) return actual.isNumber();
        if (expected.isTextual()) return actual.isTextual();
        if (expected.isBoolean()) return actual.isBoolean();
        if (expected.isObject()) return actual.isObject();
        if (expected.isArray()) return actual.isArray();
        return true; // null in the expected shape imposes no constraint
    }

    private String typeName(JsonNode node) {
        if (node == null || node.isMissingNode()) return "undefined";
        if (node.isNull()) return "null";
        if (node.isTextual()) return "string";
        if (node.isBoolean()) return "boolean";
        if (node.isInt() || node.isLong()) return "integer";
        if (node.isFloatingPointNumber()) return "number";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        return "unknown";
    }

    private String firstLine(String message) {
        if (message == null) return "unknown error";
        int idx = message.indexOf('\n');
        return idx > 0 ? message.substring(0, idx) : message;
    }
}