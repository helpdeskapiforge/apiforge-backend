package com.apiplatform.ai.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockDataHeuristicGeneratorTest {

    private final MockDataHeuristicGenerator generator = new MockDataHeuristicGenerator();
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void generatesEmailLookingValueForEmailField() throws Exception {
        JsonNode shape = objectMapper.readTree("{\"email\": \"x\"}");
        JsonNode result = objectMapper.readTree(generator.generateFromShape(shape, 1));
        assertTrue(result.get("email").asText().contains("@"));
    }

    @Test
    void preservesFieldNamesAndTypes() throws Exception {
        JsonNode shape = objectMapper.readTree("{\"id\": 1, \"name\": \"x\", \"active\": true}");
        JsonNode result = objectMapper.readTree(generator.generateFromShape(shape, 1));

        assertTrue(result.has("id"));
        assertTrue(result.get("id").isNumber());
        assertTrue(result.has("name"));
        assertTrue(result.get("name").isTextual());
        assertTrue(result.has("active"));
        assertTrue(result.get("active").isBoolean());
    }

    @Test
    void handlesNestedObjects() throws Exception {
        JsonNode shape = objectMapper.readTree("{\"user\": {\"id\": 1, \"email\": \"x\"}}");
        JsonNode result = objectMapper.readTree(generator.generateFromShape(shape, 1));

        assertTrue(result.get("user").isObject());
        assertTrue(result.get("user").has("email"));
    }

    @Test
    void generatesRequestedCountForArrayShape() throws Exception {
        JsonNode shape = objectMapper.readTree("[{\"id\": 1}]");
        JsonNode result = objectMapper.readTree(generator.generateFromShape(shape, 5));

        assertTrue(result.isArray());
        assertEquals(5, result.size());
    }

    @Test
    void producesValidJsonOutput() throws Exception {
        JsonNode shape = objectMapper.readTree("{\"id\": 1, \"createdAt\": \"2024-01-01\", \"price\": 9.99}");
        String output = generator.generateFromShape(shape, 1);
        assertDoesNotThrow(() -> objectMapper.readTree(output));
    }
}