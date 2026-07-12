package com.voxa.api.model.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
        Instant timestamp,
        boolean success,
        String message,
        String path,
        List<FieldError> errors
) {
    public record FieldError(
            String field,
            String message
    ) {

    }
}
