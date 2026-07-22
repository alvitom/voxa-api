package com.voxa.api.controller;

import com.voxa.api.model.request.*;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/v1/auth",
        produces = MediaType.APPLICATION_JSON_VALUE

)
public class AuthController {
    private final AuthService authService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> register(@Valid @RequestBody RegisterUserRequest request) throws MessagingException {
        authService.register(request);

        WebResponse<?> webResponse = WebResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(webResponse);
    }

    @PostMapping(
            value = "/verify-account",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<AuthenticationResponse>> verifyAccount(@Valid @RequestBody VerificationAccountUserRequest request) {
        AuthenticationResponse authenticationResponse = authService.verifyAccount(request.verificationToken());

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Account verified successfully")
                .data(authenticationResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/resend-verification",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> resendAccountVerification(String identifier) throws MessagingException {
        authService.resendAccountVerification(identifier);

        WebResponse<?> webResponse = WebResponse.builder()
                .success(true)
                .message("Verification has been sent. Please check your email.")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    // TODO: Resend Verification Feature

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<AuthenticationResponse>> login(@Valid @RequestBody LoginUserRequest request) {
        AuthenticationResponse authenticationResponse = authService.login(request);

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("User login successfully")
                .data(authenticationResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<AuthenticationResponse>> refresh(@Valid @RequestBody RefreshTokenUserRequest request) {
        AuthenticationResponse authenticationResponse = authService.refresh(request.refreshToken());

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Refresh token successfully")
                .data(authenticationResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/logout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> logout(@Valid @RequestBody RefreshTokenUserRequest request) {
        authService.logout(request.refreshToken());

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Logout successfully")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/forgot-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) throws MessagingException {
        authService.forgotPassword(request.identifier());

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Reset password request successful. Kindly check your email to reset the password")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.passwordResetToken());

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Reset password successfully. Please login with your new password")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }
}
