package com.voxa.api.controller;

import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<WebResponse<AuthenticationResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        AuthenticationResponse authenticationResponse = authService.register(request);

        WebResponse<AuthenticationResponse> webResponse = WebResponse.<AuthenticationResponse>builder()
                .success(true)
                .message("User registered successfully")
                .data(authenticationResponse)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(webResponse);
    }
}
