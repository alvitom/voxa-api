package com.voxa.api.exception;

import com.voxa.api.model.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(
            value = MethodArgumentNotValidException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(HttpServletRequest request,
                                                                         MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Validation error")
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(
            value = ConstraintViolationException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> constraintViolationException(HttpServletRequest request,
                                                                         ConstraintViolationException exception) {
        List<ErrorResponse.FieldError> errors = exception.getConstraintViolations()
                .stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getPropertyPath().toString(),
                        fieldError.getMessage()
                ))
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Validation error")
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(
            value = ResponseStatusException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> responseStatusException(HttpServletRequest request,
                                                                 ResponseStatusException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getReason())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(exception.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(
            value = HttpMediaTypeNotSupportedException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> httpMediaTypeNotSupportedException(HttpServletRequest request,
                                                                 HttpMediaTypeNotSupportedException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(exception.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(
            value = HttpMessageNotReadableException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> httpMessageNotReadableException(HttpServletRequest request,
                                                                         HttpMessageNotReadableException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(
            value = HttpRequestMethodNotSupportedException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> httpRequestMethodNotSupportedException(HttpServletRequest request,
                                                                                HttpRequestMethodNotSupportedException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(exception.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(
            value = MissingServletRequestParameterException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> missingServletRequestParameterException(HttpServletRequest request,
                                                                                 MissingServletRequestParameterException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(exception.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(
            value = InternalAuthenticationServiceException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> internalAuthenticationServiceException(HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Invalid credentials")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(
            value = DisabledException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> disabledException(HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Your account not verified")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(
            value = LockedException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> lockedException(HttpServletRequest request,
                                                         LockedException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Your account is locked. Please contact admin to request unlocked your account.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(
            value = BadCredentialsException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> badCredentialsException(HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message("Invalid credentials")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(
            value = JwtException.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ErrorResponse> jwtException(HttpServletRequest request,
                                                      JwtException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .success(false)
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
