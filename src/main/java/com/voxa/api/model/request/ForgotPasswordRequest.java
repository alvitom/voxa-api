package com.voxa.api.model.request;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank
        String identifier
) {
}
