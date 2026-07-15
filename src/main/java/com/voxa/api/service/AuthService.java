package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.util.TokenGenerator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final TokenGenerator tokenGenerator;

    private final MailService mailService;

    private final HashService hashService;

    public void register(RegisterUserRequest request) throws MessagingException {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists. Please use the others");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists. Please use the others");
        }

        String verificationToken = tokenGenerator.generate(20);

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(false)
                .verificationToken(hashService.hash(verificationToken))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        mailService.sendAccountVerification(user.getEmail(), user.getUsername(), verificationToken);
    }

    public AuthenticationResponse verifyAccount(String verificationToken) {
        String hashedToken = hashService.hash(verificationToken);

        User user = userRepository.findByVerificationToken(hashedToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is invalid"));

        if (LocalDateTime.now().isAfter(user.getVerificationTokenExpiredAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token was expired. Please send verification again");
        }

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User already verified.");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiredAt(null);

        userRepository.save(user);

        String token = jwtService.generate(user);

        UserResponse userResponse = UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .build();

        return AuthenticationResponse.builder()
                .token(token)
                .userResponse(userResponse)
                .build();
    }

    public AuthenticationResponse login(LoginUserRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.identifier(),
                        request.password()
                )
        );

        User user = (User) authentication.getPrincipal();

        String token = jwtService.generate(user);

        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }
}
