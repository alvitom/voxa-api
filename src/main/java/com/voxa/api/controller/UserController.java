package com.voxa.api.controller;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.DeleteUserRequest;
import com.voxa.api.model.request.DeleteVerificationRequest;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        value = "/v1/users",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping(
            value = "/me"
    )
    public ResponseEntity<WebResponse<UserResponse>> get(@AuthenticationPrincipal User user) {
        UserResponse userResponse = userService.get(user.getId());

        WebResponse<UserResponse> webResponse = WebResponse.<UserResponse>builder()
                .success(true)
                .message("Get current user successfully")
                .data(userResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PatchMapping(
            value = "/me",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<UserResponse>> update(@AuthenticationPrincipal User user, @RequestBody UpdateUserRequest request) {
        UserResponse userResponse = userService.update(user.getId(), request);

        WebResponse<UserResponse> webResponse = WebResponse.<UserResponse>builder()
                .success(true)
                .message("Update user successfully")
                .data(userResponse)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @PostMapping(
            value = "/me/send-delete-verification",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> sendDeleteVerification(@AuthenticationPrincipal User user, @Valid @RequestBody DeleteVerificationRequest request) throws MessagingException {
        userService.sendDeleteVerification(user.getId(), request.password());

        WebResponse<?> webResponse = WebResponse.builder()
                .success(true)
                .message("Delete verification request successfully. Please check your inbox email to get the verification code")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }

    @DeleteMapping(
            value = "/me",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> delete(@AuthenticationPrincipal User user, @Valid @RequestBody DeleteUserRequest request) {
        userService.delete(user.getId(), request.verificationCode());

        WebResponse<?> webResponse = WebResponse.builder()
                .success(true)
                .message("Delete user successfully")
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(webResponse);
    }
}
