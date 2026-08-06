package com.voxa.api.model.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequest(
        @NotBlank
        String password
) {
}
