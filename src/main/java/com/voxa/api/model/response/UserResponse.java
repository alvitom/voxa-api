package com.voxa.api.model.response;

import lombok.Builder;

@Builder
public record UserResponse(
        String email,
        String username,
        String name
) {
}
