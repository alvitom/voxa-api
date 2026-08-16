package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.entity.UserProfile;
import com.voxa.api.model.projection.UserProjection;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.util.TokenGenerator;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenGenerator tokenGenerator;

    private final HashService hashService;

    private final MailService mailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenGenerator tokenGenerator,
                       @Qualifier("hs256")
                       HashService hashService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.hashService = hashService;
        this.mailService = mailService;
    }

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

    public void sendDeleteVerification(String id, String password) throws MessagingException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean matches = passwordEncoder.matches(password, user.getPassword());

        if (!matches) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is wrong");
        }

        String verificationCode = tokenGenerator.generate(3);

        user.setDeleteVerificationCode(hashService.hash(verificationCode));
        user.setDeleteVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(30));

        userRepository.save(user);

        mailService.sendDeleteVerification(user.getEmail(), user.getUsername(), verificationCode);
    }

    public void delete(String id, String verificationCode) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String hashedVerificationCode = hashService.hash(verificationCode);

        if (!user.getDeleteVerificationCode().equals(hashedVerificationCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid verification code");
        }

        if (LocalDateTime.now().isAfter(user.getDeleteVerificationCodeExpiredAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verification code was expired");
        }

        userRepository.delete(user);
    }
}
