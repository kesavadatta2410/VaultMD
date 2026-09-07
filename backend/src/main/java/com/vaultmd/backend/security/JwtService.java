package com.vaultmd.backend.security;

import com.vaultmd.backend.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${vaultmd.jwt.secret}") String secret,
                       @Value("${vaultmd.jwt.expiration-ms}") long expirationMs) {
        // HS256 needs a key of at least 256 bits (32 bytes). The configured
        // secret is treated as raw UTF-8 bytes, so it must be >= 32 characters.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(email)
                // Stored as a string, not a number: jjwt's Jackson (de)serializer can
                // round-trip small numeric claims as Integer instead of Long, which
                // throws a ClassCastException on claims.get("userId", Long.class).
                // A string claim sidesteps that entirely.
                .claim("userId", String.valueOf(userId))
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                // signWith(key) without an explicit algorithm lets JJWT infer HS256
                // from the SecretKey type — avoids the deprecated SignatureAlgorithm enum.
                .signWith(signingKey)
                .compact();
    }

    /** Returns null if the token is missing, malformed, expired, or has an invalid signature. */
    public UserPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Long userId = Long.valueOf(claims.get("userId", String.class));
            String email = claims.getSubject();
            Role role = Role.valueOf(claims.get("role", String.class));
            return new UserPrincipal(userId, email, role);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
