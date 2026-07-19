package com.menzo.menzo.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.menzo.menzo.config.JwtProperties;
import com.menzo.menzo.domain.user.Role;
import com.menzo.menzo.domain.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(padTo32Bytes(secretBytes));
    }

    private static byte[] padTo32Bytes(byte[] input) {
        if (input.length >= 32) {
            return input;
        }
        byte[] padded = new byte[32];
        System.arraycopy(input, 0, padded, 0, input.length);
        return padded;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.getAccessTokenTtlMinutes()));
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Instant accessTokenExpiry() {
        return Instant.now().plus(Duration.ofMinutes(properties.getAccessTokenTtlMinutes()));
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(properties.getRefreshTokenTtlDays());
    }

    public DecodedAccessToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            return new DecodedAccessToken(userId, claims.get("email", String.class), role);
        } catch (ExpiredJwtException e) {
            throw new JwtException("El token de acceso ha expirado", e);
        } catch (Exception e) {
            throw new JwtException("Token de acceso inválido", e);
        }
    }

    public record DecodedAccessToken(UUID userId, String email, Role role) {
    }
}
