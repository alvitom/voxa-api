package com.voxa.api.model.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenUserRequest(
        @NotBlank
        String refreshToken
) {
}
