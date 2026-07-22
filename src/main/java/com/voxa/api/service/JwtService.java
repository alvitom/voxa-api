package com.voxa.api.service;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import javax.crypto.SecretKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    private String generate(Map<String, Object> claims, User user, Duration expiration) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.issuer())
                .subject(user.getId())
                .claims(claims)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(expiration)))
                .signWith(getSignKey())
                .compact();
    }

    public String generate(User user, Duration expiration) {
        return generate(new HashMap<>(), user, expiration);
    }

    public String generate(User user, String type, Duration expiration) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("type", type);

        return generate(claims, user, expiration);
    }

    public String getSubject(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    public String getType(String token) {
        Claims claims = extractClaims(token);
        return claims.get("type", String.class);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        byte[] decodedKey = Base64.getDecoder().decode(jwtProperties.secretKey());

        return Keys.hmacShaKeyFor(decodedKey);
    }
}
