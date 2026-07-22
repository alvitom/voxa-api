package com.voxa.api.model.request;

import jakarta.validation.constraints.NotBlank;

public record VerificationAccountUserRequest(
        @NotBlank
        String verificationToken
) {
}
