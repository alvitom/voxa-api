package com.voxa.api.controller;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.UserService;
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
}
