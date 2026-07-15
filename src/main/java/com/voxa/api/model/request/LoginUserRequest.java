package com.voxa.api.model.request;

import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(
        @NotBlank
        String identifier,
        @NotBlank
        String password
) {
}
