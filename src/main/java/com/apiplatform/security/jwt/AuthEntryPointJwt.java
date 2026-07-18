package com.apiplatform.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Entry point invoked whenever an unauthenticated request hits a protected endpoint.
 * <p>
 * This runs inside the Spring Security filter chain, *before* the request ever reaches
 * a controller, so {@code GlobalExceptionHandler} never sees it. Previously this called
 * {@code response.sendError(...)}, which hands control to the servlet container's
 * default error page and produces a response shaped completely differently from the
 * rest of the API's error envelope. Writing the body directly (as a small hand-built
 * JSON string, deliberately avoiding a dependency on any particular Jackson major
 * version/package here) keeps every error response consistent for clients.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        log.debug("Unauthenticated request to {} {}: {}", request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String path = escape(request.getRequestURI());
        String json = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"Authentication is required to access this resource.","path":"%s"}""".formatted(Instant.now(), path);

        response.getWriter().write(json);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
