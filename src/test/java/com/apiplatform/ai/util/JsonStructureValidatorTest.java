package com.apiplatform.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonStructureValidatorTest {

    private final JsonStructureValidator validator = new JsonStructureValidator();

    @Test
    void validSyntaxWithNoExpectedSchemaIsFullyValid() {
        JsonStructureValidator.Result result = validator.validate("{\"a\": 1, \"b\": \"x\"}", null);
        assertTrue(result.syntaxValid());
        assertTrue(result.isFullyValid());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void malformedJsonIsReportedAsSyntaxInvalid() {
        JsonStructureValidator.Result result = validator.validate("{\"a\": 1,,}", null);
        assertFalse(result.syntaxValid());
        assertFalse(result.isFullyValid());
        assertFalse(result.issues().isEmpty());
    }

    @Test
    void emptyInputIsInvalid() {
        JsonStructureValidator.Result result = validator.validate("", null);
        assertFalse(result.syntaxValid());
        assertFalse(result.issues().isEmpty());
    }

    @Test
    void detectsMissingField() {
        String expected = "{\"id\": 1, \"name\": \"x\", \"email\": \"x@example.com\"}";
        String actual = "{\"id\": 1, \"name\": \"x\"}";

        JsonStructureValidator.Result result = validator.validate(actual, expected);

        assertTrue(result.syntaxValid());
        assertFalse(result.isFullyValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.message().contains("email")));
    }

    @Test
    void detectsTypeMismatch() {
        String expected = "{\"age\": 30}";
        String actual = "{\"age\": \"thirty\"}";

        JsonStructureValidator.Result result = validator.validate(actual, expected);

        assertFalse(result.isFullyValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.message().contains("type")));
    }

    @Test
    void detectsUnexpectedExtraField() {
        String expected = "{\"id\": 1}";
        String actual = "{\"id\": 1, \"secretDebugFlag\": true}";

        JsonStructureValidator.Result result = validator.validate(actual, expected);

        assertFalse(result.isFullyValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.message().contains("secretDebugFlag")));
    }

    @Test
    void nestedObjectFieldsAreCheckedRecursively() {
        String expected = "{\"user\": {\"id\": 1, \"email\": \"x@example.com\"}}";
        String actual = "{\"user\": {\"id\": 1}}";

        JsonStructureValidator.Result result = validator.validate(actual, expected);

        assertFalse(result.isFullyValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.path().equals("$.user.email")));
    }

    @Test
    void matchingShapeWithDifferentValuesIsStillValid() {
        String expected = "{\"id\": 1, \"name\": \"template\"}";
        String actual = "{\"id\": 42, \"name\": \"Ada Lovelace\"}";

        JsonStructureValidator.Result result = validator.validate(actual, expected);

        assertTrue(result.isFullyValid());
    }
}
