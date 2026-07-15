package com.voxa.api.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class TokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(int size) {
        byte[] bytes = new byte[size];

        secureRandom.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }
}
