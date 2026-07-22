package com.voxa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(value = "spring.application.security.jwt")
@Validated
public record JwtProperties(
        String issuer,
        String secretKey,
        Duration expiration,
        Duration refreshTokenExpiration
) {
}
