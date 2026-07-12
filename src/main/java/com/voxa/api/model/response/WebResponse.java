package com.voxa.api.model.response;

import lombok.Builder;

@Builder
public record WebResponse<T>(
        boolean success,
        String message,
        T data
) {
}
