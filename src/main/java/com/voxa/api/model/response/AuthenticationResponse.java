package com.voxa.api.model.response;

import lombok.Builder;

@Builder
public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
