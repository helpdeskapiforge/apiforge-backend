package com.apiplatform.ai.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A small, dependency-free "faker": generates realistic-looking values by matching JSON
 * field names against common patterns (email, name, id, date, price, ...).
 * <p>
 * This exists so the Mock Data Generator still works with zero AI configuration, as long
 * as the caller supplies a JSON shape (an example object) rather than pure prose --
 * {@code AIGenerationService} uses this as a fallback when no {@code AIProvider} is
 * available, and as a sanity-repair pass if the AI's own JSON output fails to parse.
 * Pure natural-language descriptions ("a user with an email") still require an AI
 * provider, since turning prose into a schema is exactly the part a fixed rule set can't do.
 */
@Component
public class MockDataHeuristicGenerator {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final Random random = new Random();

    private static final Pattern EMAIL = Pattern.compile("email", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("(^|_)name$|fullname|full_name", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_NAME = Pattern.compile("first_?name", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAST_NAME = Pattern.compile("last_?name", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID = Pattern.compile("(^|_)id$|^id", Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_FIELD = Pattern.compile("uuid|guid", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("date|_at$|At$|timestamp", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE = Pattern.compile("price|amount|cost|total", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("phone|mobile|tel", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_FIELD = Pattern.compile("url|link|href|website", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOL_FIELD = Pattern.compile("^is_?|^has_?|active|enabled", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTRY = Pattern.compile("country", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITY = Pattern.compile("city", Pattern.CASE_INSENSITIVE);

    private static final String[] FIRST_NAMES = {"Olivia", "Liam", "Emma", "Noah", "Ava", "Mateo", "Sofia", "Ethan", "Priya", "Kenji"};
    private static final String[] LAST_NAMES = {"Nguyen", "Garcia", "Smith", "Kowalski", "Okafor", "Tanaka", "Fernandez", "Patel", "Kim", "Rossi"};
    private static final String[] CITIES = {"Austin", "Lisbon", "Nairobi", "Bengaluru", "Toronto", "Osaka", "Wroclaw", "Auckland"};
    private static final String[] COUNTRIES = {"United States", "Portugal", "Kenya", "India", "Canada", "Japan", "Poland", "New Zealand"};
    private static final String[] DOMAINS = {"example.com", "mail.dev", "testmail.io"};

    /**
     * @param exampleJson a JSON object/array whose field names describe the desired shape
     * @param count       how many items to generate when the input is (or should become) an array
     * @return realistic values in the same shape, as pretty-printed JSON text
     */
    public String generateFromShape(JsonNode exampleJson, int count) {
        if (exampleJson.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            JsonNode template = exampleJson.isEmpty() ? objectMapper.createObjectNode() : exampleJson.get(0);
            int n = Math.max(1, Math.min(count, 100));
            for (int i = 0; i < n; i++) {
                result.add(fillObject(template));
            }
            return writePretty(result);
        }
        return writePretty(fillObject(exampleJson));
    }

    private JsonNode fillObject(JsonNode template) {
        if (!template.isObject()) {
            return valueFor("value", template);
        }
        ObjectNode result = objectMapper.createObjectNode();

        // JACKSON 3 FIX: Use properties() instead of fields()
        for (Map.Entry<String, JsonNode> entry : template.properties()) {
            String field = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isObject()) {
                result.set(field, fillObject(value));
            } else if (value.isArray()) {
                ArrayNode arr = objectMapper.createArrayNode();
                JsonNode elementTemplate = value.isEmpty() ? objectMapper.createObjectNode() : value.get(0);
                int n = 1 + random.nextInt(3);
                for (int i = 0; i < n; i++) arr.add(fillObject(elementTemplate));
                result.set(field, arr);
            } else {
                result.set(field, valueFor(field, value));
            }
        }
        return result;
    }

    private JsonNode valueFor(String field, JsonNode original) {
        if (matches(UUID_FIELD, field)) return objectMapper.getNodeFactory().textNode(UUID.randomUUID().toString());
        if (matches(FIRST_NAME, field)) return textNode(pick(FIRST_NAMES));
        if (matches(LAST_NAME, field)) return textNode(pick(LAST_NAMES));
        if (matches(NAME, field)) return textNode(pick(FIRST_NAMES) + " " + pick(LAST_NAMES));
        if (matches(EMAIL, field)) return textNode((pick(FIRST_NAMES) + "." + pick(LAST_NAMES) + random.nextInt(100)).toLowerCase() + "@" + pick(DOMAINS));
        if (matches(PHONE, field)) return textNode(String.format("+1-555-%04d", random.nextInt(10000)));
        if (matches(URL_FIELD, field)) return textNode("https://" + pick(DOMAINS) + "/" + field.toLowerCase());
        if (matches(DATE, field)) return textNode(Instant.now().minusSeconds(random.nextInt(365 * 24 * 3600)).toString());
        if (matches(PRICE, field)) return objectMapper.getNodeFactory().numberNode(Math.round(random.nextDouble() * 500 * 100) / 100.0);
        if (matches(COUNTRY, field)) return textNode(pick(COUNTRIES));
        if (matches(CITY, field)) return textNode(pick(CITIES));
        if (matches(BOOL_FIELD, field) || original.isBoolean()) return objectMapper.getNodeFactory().booleanNode(random.nextBoolean());
        if (matches(ID, field)) return original.isTextual() ? textNode(UUID.randomUUID().toString()) : objectMapper.getNodeFactory().numberNode(1 + random.nextInt(9999));

        if (original.isIntegralNumber()) return objectMapper.getNodeFactory().numberNode(random.nextInt(1000));
        if (original.isFloatingPointNumber()) return objectMapper.getNodeFactory().numberNode(Math.round(random.nextDouble() * 1000 * 100) / 100.0);
        if (original.isBoolean()) return objectMapper.getNodeFactory().booleanNode(random.nextBoolean());
        if (original.isNull()) return objectMapper.getNodeFactory().nullNode();
        return textNode(field + "_" + (1 + random.nextInt(9999)));
    }

    private boolean matches(Pattern p, String field) {
        return p.matcher(field).find();
    }

    // JACKSON 3 FIX: Changed return type from 'TextNode' to 'JsonNode' to match factory return type
    private JsonNode textNode(String value) {
        return objectMapper.getNodeFactory().textNode(value);
    }

    private String pick(String[] options) {
        return options[random.nextInt(options.length)];
    }

    private String writePretty(JsonNode node) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return node.toString();
        }
    }
}