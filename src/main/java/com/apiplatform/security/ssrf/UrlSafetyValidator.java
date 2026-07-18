package com.apiplatform.security.ssrf;

import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Guards the request-proxy feature ({@code POST /api/proxy/execute}) against
 * Server-Side Request Forgery.
 * <p>
 * Before this existed, the proxy controller took a user-supplied URL and fetched it
 * verbatim with {@code RestTemplate} from the backend's network position. An attacker
 * (or a curious user) could point it at {@code http://169.254.169.254/...} (cloud
 * instance metadata), {@code http://localhost:5432} (the database), internal admin
 * panels, or any other host reachable only from inside the deployment's network.
 * <p>
 * <b>Known limitation:</b> this performs a "time of check" DNS resolution. A
 * sufficiently determined attacker could attempt a DNS-rebinding attack (resolve to a
 * public IP during validation, then to a private IP during the actual connection).
 * Fully closing that gap requires pinning the resolved IP for the connection itself
 * (e.g. a custom {@code DnsResolver} on the HTTP client) — tracked as a follow-up
 * hardening item; this validator still blocks the overwhelming majority of real-world
 * SSRF attempts and all of the "obvious" ones (localhost, metadata endpoint, private
 * ranges, non-HTTP schemes).
 */
@Component
public class UrlSafetyValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public void assertSafe(String rawUrl) {
        URI uri = parse(rawUrl);

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Only http/https URLs are allowed.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must include a host.");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URLs with embedded credentials are not allowed.");
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host: " + host);
        }

        if (isBlockedAddress(address)) {
            throw new IllegalArgumentException("Requests to private, loopback, link-local, or metadata addresses are not allowed.");
        }
    }

    private URI parse(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed URL.");
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }

        // Cloud metadata services (AWS/GCP/Azure/DigitalOcean all use this link-local IP).
        if (address instanceof Inet4Address && address.getHostAddress().equals("169.254.169.254")) {
            return true;
        }

        // Unique local IPv6 (fc00::/7) is the IPv6 equivalent of RFC1918 private space.
        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();
            if ((bytes[0] & 0xFE) == 0xFC) {
                return true;
            }
        }

        return false;
    }
}
