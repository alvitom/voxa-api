package com.voxa.api.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @Value("${spring.application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${spring.application.security.jwt.expiration}")
    private Duration expiration;

    @Value("${spring.application.security.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    public String generate(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(expiration)))
                .signWith(getSignKey())
                .compact();
    }

    public String generate(UserDetails userDetails) {
        return generate(new HashMap<>(), userDetails);
    }

    private SecretKey getSignKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);

        return Keys.hmacShaKeyFor(decodedKey);
    }
}
