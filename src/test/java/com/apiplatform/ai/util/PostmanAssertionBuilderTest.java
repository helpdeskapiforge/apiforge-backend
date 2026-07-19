package com.apiplatform.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostmanAssertionBuilderTest {

    private final PostmanAssertionBuilder builder = new PostmanAssertionBuilder();

    @Test
    void generatesStatusCodeAssertion() {
        String script = builder.build("GET", 200, "{\"id\": 1}");
        assertTrue(script.contains("pm.response.to.have.status(200)"));
    }

    @Test
    void defaultsTo200WhenStatusCodeMissing() {
        String script = builder.build("GET", null, "{\"id\": 1}");
        assertTrue(script.contains("pm.response.to.have.status(200)"));
    }

    @Test
    void generatesTypeCheckForEachTopLevelField() {
        String script = builder.build("POST", 201, "{\"id\": 1, \"name\": \"Ada\", \"active\": true}");
        assertTrue(script.contains("json[\"id\"]"));
        assertTrue(script.contains("to.be.a('number')"));
        assertTrue(script.contains("json[\"name\"]"));
        assertTrue(script.contains("to.be.a('string')"));
        assertTrue(script.contains("json[\"active\"]"));
        assertTrue(script.contains("to.be.a('boolean')"));
    }

    @Test
    void handlesArrayResponses() {
        String script = builder.build("GET", 200, "[{\"id\": 1}, {\"id\": 2}]");
        assertTrue(script.contains("Array.isArray(json)"));
        assertTrue(script.contains("item[\"id\"]"));
    }

    @Test
    void gracefullyHandlesNonJsonBody() {
        String script = builder.build("GET", 200, "not json");
        assertTrue(script.contains("pm.response.to.have.status(200)"));
        assertTrue(script.contains("not valid JSON"));
    }

    @Test
    void alwaysIncludesResponseTimeAssertion() {
        String script = builder.build("GET", 200, "{}");
        assertTrue(script.contains("responseTime"));
    }
}
