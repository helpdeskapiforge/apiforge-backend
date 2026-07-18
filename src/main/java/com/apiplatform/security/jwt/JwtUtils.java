package com.apiplatform.security.jwt;

import com.apiplatform.security.services.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    /** HS256 requires a key of at least 256 bits (32 bytes) once Base64-decoded. */
    private static final int MIN_KEY_BYTES = 32;

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void validateSecret() {
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwtSecret must be valid Base64.", e);
        }
        if (decoded.length < MIN_KEY_BYTES) {
            // Fail fast at startup rather than silently issuing weak, brute-forceable tokens.
            throw new IllegalStateException(
                    "app.jwtSecret is too short once decoded (" + decoded.length +
                            " bytes). HS256 requires at least " + MIN_KEY_BYTES + " bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(decoded);
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        Date now = new Date();
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}
