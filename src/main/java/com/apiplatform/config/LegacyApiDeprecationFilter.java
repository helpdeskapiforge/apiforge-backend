package com.apiplatform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Every REST controller now answers on both `/api/v1/...` (current) and the original
 * unversioned `/api/...` path (kept temporarily so existing clients don't break in
 * place). This filter marks responses on the legacy path as deprecated using the
 * standard {@code Deprecation}/{@code Sunset} headers (RFC 8594) so API consumers -
 * and this repo's own frontend - have a machine-readable signal to migrate off it
 * before it's removed.
 * <p>
 * When the legacy path is actually removed, delete this filter along with the
 * duplicate path arrays on each {@code @RequestMapping}.
 */
@Component
public class LegacyApiDeprecationFilter extends OncePerRequestFilter {

    private static final String SUNSET_DATE = "Wed, 31 Dec 2026 23:59:59 GMT";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isLegacyVersionedApi = path.startsWith("/api/")
                && !path.startsWith("/api/v1/")
                && !path.startsWith("/api/mock/simulator/"); // intentionally unversioned, not "legacy"

        if (isLegacyVersionedApi) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", SUNSET_DATE);
            response.setHeader("Link", "</api/v1" + path.substring(4) + ">; rel=\"successor-version\"");
        }

        chain.doFilter(request, response);
    }
}
