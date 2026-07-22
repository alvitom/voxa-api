package com.voxa.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("spring.application.mail")
@Validated
public record MailProperties(
        String targetUrl,
        String sender
) {
}
