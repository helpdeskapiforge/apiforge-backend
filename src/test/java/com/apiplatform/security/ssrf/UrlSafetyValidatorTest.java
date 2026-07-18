package com.apiplatform.security.ssrf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/admin",
            "http://127.0.0.1/secrets",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/",
            "http://10.0.0.5/internal",
            "http://192.168.1.1/router",
            "ftp://example.com/file",
            "file:///etc/passwd",
            "http://user:pass@example.com/"
    })
    void rejectsUnsafeTargets(String url) {
        assertThrows(IllegalArgumentException.class, () -> validator.assertSafe(url));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> validator.assertSafe("not a url"));
    }

    @Test
    void allowsOrdinaryPublicHttpsUrl() {
        // example.com resolves to a public IP address.
        assertDoesNotThrow(() -> validator.assertSafe("https://example.com/api/ping"));
    }
}
