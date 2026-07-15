package com.voxa.api.controller;

import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/v1/auth",
        produces = MediaType.APPLICATION_JSON_VALUE

)
@Validated
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
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<AuthenticationResponse>> verifyAccount(@NotBlank @RequestParam("token") String token) {
        AuthenticationResponse authenticationResponse = authService.verifyAccount(token);

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("Account verified successfully")
                .data(authenticationResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

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
}
