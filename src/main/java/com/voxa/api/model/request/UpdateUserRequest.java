package com.voxa.api.model.request;

public record UpdateUserRequest(
        String name,
        String bio,
        String ppUrl
) {
}
