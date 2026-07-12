package com.voxa.api.model.request;

import jakarta.validation.constraints.*;

public record RegisterUserRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
        @NotBlank
        @Size(min = 10, max = 15)
        String phoneNumber,
        @NotBlank
        @Size(min = 3, max = 30)
        String username,
        @NotBlank
        @Size(min = 6, max = 255)
        String password
) {
}
