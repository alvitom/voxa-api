package com.voxa.api.service;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.util.TokenGenerator;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    private final TokenGenerator tokenGenerator;

    private final MailService mailService;

    private final HashService sha256Service;

    private final HashService hs256Service;

    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       TokenGenerator tokenGenerator,
                       MailService mailService,
                       @Qualifier("sha256")
                       HashService sha256Service,
                       @Qualifier("hs256")
                       HashService hs256Service,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenGenerator = tokenGenerator;
        this.mailService = mailService;
        this.sha256Service = sha256Service;
        this.hs256Service = hs256Service;
        this.jwtProperties = jwtProperties;
    }

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
                .verificationToken(hs256Service.hash(verificationToken))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        mailService.sendAccountVerification(user.getEmail(), user.getUsername(), verificationToken);
    }

    public AuthenticationResponse verifyAccount(String verificationToken) {
        String hashedToken = hs256Service.hash(verificationToken);

        User user = userRepository.findByVerificationToken(hashedToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is invalid"));

        if (LocalDateTime.now().isAfter(user.getVerificationTokenExpiredAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token was expired. Please send verification again");
        }

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User already verified.");
        }

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String accessToken = jwtService.generate(user, "access-token", accessTokenExpiration);

        String refreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiredAt(null);
        user.setRefreshToken(sha256Service.hash(refreshToken));
        user.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(user);

        UserResponse userResponse = UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    public void resendAccountVerification(String identifier) throws MessagingException {
        User user = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User already verified");
        }

        String verificationToken = tokenGenerator.generate(20);

        user.setVerificationToken(hs256Service.hash(verificationToken));
        user.setVerificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30));

        userRepository.save(user);

        mailService.sendAccountVerification(user.getEmail(), user.getUsername(), verificationToken);
    }

    public AuthenticationResponse login(LoginUserRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.identifier(),
                        request.password()
                )
        );

        User user = (User) authentication.getPrincipal();

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String accessToken = jwtService.generate(user, "access-token", accessTokenExpiration);
        String refreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);

        user.setRefreshToken(sha256Service.hash(refreshToken));
        user.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(user);

        UserResponse userResponse = UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    public AuthenticationResponse refresh(String refreshToken) {
        String type = jwtService.getType(refreshToken);

        if (!type.equals("refresh-token")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token type is invalid");
        }

        String userId = jwtService.getSubject(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refresh token not found"));

        String hashedRefreshToken = sha256Service.hash(refreshToken);

        if (!user.getRefreshToken().equals(hashedRefreshToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is invalid");
        }

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String newAccessToken = jwtService.generate(user, "access-token", accessTokenExpiration);
        String newRefreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);

        user.setRefreshToken(sha256Service.hash(newRefreshToken));
        user.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(user);

        UserResponse userResponse = UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userResponse)
                .build();
    }

    public void logout(String refreshToken) {
        String type = jwtService.getType(refreshToken);

        if (!type.equals("refresh-token")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token type is invalid");
        }

        String userId = jwtService.getSubject(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refresh token not found"));

        String hashedRefreshToken = sha256Service.hash(refreshToken);

        if (!user.getRefreshToken().equals(hashedRefreshToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is invalid");
        }

        user.setRefreshToken(null);
        user.setRefreshTokenExpiredAt(null);

        userRepository.save(user);
    }

    public void forgotPassword(String identifier) throws MessagingException {
        User user = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String passwordResetToken = tokenGenerator.generate(20);

        user.setPasswordResetToken(hs256Service.hash(passwordResetToken));
        user.setPasswordResetTokenExpiredAt(LocalDateTime.now().plusMinutes(30));

        userRepository.save(user);

        mailService.sendResetPassword(user.getEmail(), user.getUsername(), passwordResetToken);
    }

    public void resetPassword(String passwordResetToken) {
        String hashedPasswordResetToken = hs256Service.hash(passwordResetToken);

        User user = userRepository.findByPasswordResetToken(hashedPasswordResetToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is invalid"));

        if (LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiredAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token was expired. Please request again");
        }

        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiredAt(null);

        userRepository.save(user);
    }
}
