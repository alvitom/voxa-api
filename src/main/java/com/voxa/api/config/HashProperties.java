package com.voxa.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(value = "spring.application.security.hash")
@Validated
public record HashProperties(
        @NotBlank
        @Size(min = 32)
        String secretKey
) {
}
