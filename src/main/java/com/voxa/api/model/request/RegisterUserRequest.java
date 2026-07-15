package com.voxa.api.model.request;

import com.voxa.api.validation.PasswordMatches;
import jakarta.validation.constraints.*;

@PasswordMatches
public record RegisterUserRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
        @NotBlank
        @Size(min = 3, max = 30)
        String username,
        @NotBlank
        @Size(min = 6, max = 255)
        String password,
        @NotBlank
        String confirmPassword
) {
}
