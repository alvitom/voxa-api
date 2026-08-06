package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.entity.UserProfile;
import com.voxa.api.model.projection.UserProjection;
import com.voxa.api.model.request.DeleteUserRequest;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserResponse get(String id) {
        UserProjection user = userRepository.findCurrentUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .name(user.getName())
                .build();
    }

    public UserResponse update(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserProfile profile = user.getProfile();

        profile.setName(request.name());
        profile.setBio(request.bio());
        profile.setPpUrl(request.ppUrl());

        user.setProfile(profile);

        User updatedUser = userRepository.save(user);

        return UserResponse.builder()
                .email(updatedUser.getEmail())
                .username(updatedUser.getUsername())
                .name(profile.getName())
                .build();
    }

    public void sendDeleteVerification(String id, DeleteUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean matches = passwordEncoder.matches(request.password(), user.getPassword());

        if (!matches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is wrong");
        }

    }

    public void delete(String id, String code) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userRepository.delete(user);
    }
}
