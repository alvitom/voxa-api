package com.voxa.api.service;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.util.TokenGenerator;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private HashService hashService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    /**
     * Register Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        // When
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Email already exists"));

        verify(userRepository).existsByEmail(request.email());

        verifyNoInteractions(
                tokenGenerator,
                passwordEncoder,
                hashService,
                mailService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenUsernameAlreadyExists() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        // When
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        // Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Username already exists"));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByUsername(request.username());

        verifyNoInteractions(
                tokenGenerator,
                passwordEncoder,
                hashService,
                mailService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenRegisterIsSuccess() throws MessagingException {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "081234567890",
                "example",
                "password"
        );

        String verificationToken = "verification-token";

        String hashedPassword = "hashed-password";

        String hashedVerificationToken = "hashed-verification-token";

        // When
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(tokenGenerator.generate(anyInt())).thenReturn(verificationToken);
        when(passwordEncoder.encode(request.password())).thenReturn(hashedPassword);
        when(hashService.hash(verificationToken)).thenReturn(hashedVerificationToken);

        // Then
        authService.register(request);

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByUsername(request.username());
        verify(tokenGenerator).generate(anyInt());
        verify(passwordEncoder).encode(request.password());
        verify(hashService).hash(verificationToken);

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userArgumentCaptor.capture());

        User userArgumentCaptorValue = userArgumentCaptor.getValue();

        assertEquals(hashedPassword, userArgumentCaptorValue.getPassword());
        assertEquals(hashedVerificationToken, userArgumentCaptorValue.getVerificationToken());

        verify(mailService).sendAccountVerification(
                userArgumentCaptorValue.getEmail(),
                userArgumentCaptorValue.getUsername(),
                verificationToken);
    }

    /**
     * Verify Account Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenVerificationTokenIsInvalid() {
        String verificationToken = "verification-token";

        String hashedVerificationToken = "hashed-verification-token";

        when(hashService.hash(verificationToken)).thenReturn(hashedVerificationToken);
        when(userRepository.findByVerificationToken(hashedVerificationToken)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.verifyAccount(verificationToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Token is invalid", exception.getReason());

        verify(hashService).hash(verificationToken);
        verify(userRepository).findByVerificationToken(hashedVerificationToken);

        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(
                jwtService,
                jwtProperties
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenVerificationTokenWasExpired() {
        String verificationToken = "verification-token";

        String hashedVerificationToken = "hashed-verification-token";

        User user = User.builder()
                .verificationToken(hashedVerificationToken)
                .verificationTokenExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(hashService.hash(verificationToken)).thenReturn(hashedVerificationToken);
        when(userRepository.findByVerificationToken(hashedVerificationToken)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.verifyAccount(verificationToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Token was expired"));

        verify(hashService).hash(verificationToken);
        verify(userRepository).findByVerificationToken(hashedVerificationToken);

        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(
                jwtService,
                jwtProperties
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenUserAlreadyVerified() {
        String verificationToken = "verification-token";

        String encodedVerificationToken = "hashed-verification-token";

        User user = User.builder()
                .verificationToken(encodedVerificationToken)
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .isEnabled(true)
                .build();

        when(hashService.hash(verificationToken)).thenReturn(encodedVerificationToken);
        when(userRepository.findByVerificationToken(encodedVerificationToken)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.verifyAccount(verificationToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User already verified"));

        verify(hashService).hash(verificationToken);
        verify(userRepository).findByVerificationToken(encodedVerificationToken);

        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(
                jwtService,
                jwtProperties
        );
    }

    @Test
    void shouldReturnAuthenticationResponseWhenVerifyAccountIsSuccess() {
        String verificationToken = "verification-token";

        String hashedVerificationToken = "hashed-verification-token";

        User user = User.builder()
                .verificationToken(hashedVerificationToken)
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .isEnabled(false)
                .build();

        String refreshToken = "refresh-token";

        Duration jwtExpiration = Duration.ofMinutes(5);
        Duration jwtRefreshTokenExpiration = Duration.ofDays(1);

        when(hashService.hash(verificationToken)).thenReturn(hashedVerificationToken);
        when(userRepository.findByVerificationToken(hashedVerificationToken)).thenReturn(Optional.of(user));
        when(jwtProperties.expiration()).thenReturn(jwtExpiration);
        when(jwtProperties.refreshTokenExpiration()).thenReturn(jwtRefreshTokenExpiration);
        when(jwtService.generate(user, "access-token", jwtExpiration)).thenReturn("token");
        when(jwtService.generate(user, "refresh-token", jwtRefreshTokenExpiration)).thenReturn(refreshToken);
        when(hashService.hash(refreshToken)).thenReturn("hashed-refresh-token");

        AuthenticationResponse authenticationResponse = authService.verifyAccount(verificationToken);

        assertNotNull(authenticationResponse.accessToken());
        assertNotNull(authenticationResponse.refreshToken());
        assertEquals(user.getEmail(), authenticationResponse.user().email());
        assertEquals(user.getUsername(), authenticationResponse.user().username());
        assertNull(authenticationResponse.user().name());

        assertTrue(user.isEnabled());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiredAt());
        assertNotNull(user.getRefreshToken());
        assertNotNull(user.getRefreshTokenExpiredAt());

        verify(hashService).hash(verificationToken);
        verify(userRepository).findByVerificationToken(hashedVerificationToken);
        verify(jwtProperties).expiration();
        verify(jwtProperties).refreshTokenExpiration();
        verify(jwtService, times(2)).generate(any(User.class), anyString(), any(Duration.class));
        verify(userRepository).save(user);
    }


    /**
     * Resend Account Verification Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenResendAccountVerificationUserNotFound() {
        String identifier = "example";

        when(userRepository.findByEmailOrUsername(identifier, identifier))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resendAccountVerification(identifier));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());

        verify(userRepository).findByEmailOrUsername(identifier, identifier);

        verifyNoInteractions(
                tokenGenerator,
                hashService,
                mailService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenResendAccountVerificationUserAlreadyVerified() {
        String identifier = "example";

        User user = User.builder()
                .isEnabled(true)
                .build();

        when(userRepository.findByEmailOrUsername(identifier, identifier))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resendAccountVerification(identifier));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("User already verified", exception.getReason());

        verify(userRepository).findByEmailOrUsername(identifier, identifier);

        verifyNoInteractions(
                tokenGenerator,
                hashService,
                mailService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenResendAccountVerificationIsSuccess() throws MessagingException {
        String identifier = "example";

        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .isEnabled(false)
                .build();

        String newVerificationToken = "new-verification-token";

        when(userRepository.findByEmailOrUsername(identifier, identifier))
                .thenReturn(Optional.of(user));

        when(tokenGenerator.generate(anyInt())).thenReturn(newVerificationToken);

        authService.resendAccountVerification(identifier);

        verify(userRepository).findByEmailOrUsername(identifier, identifier);
        verify(tokenGenerator).generate(anyInt());
        verify(hashService).hash(newVerificationToken);
        verify(userRepository).save(user);
        verify(mailService).sendAccountVerification(anyString(), anyString(), anyString());
    }


    /**
     * Login Test
     */
    @Test
    void shouldThrowInternalAuthenticationServiceExceptionWhenUserNotFound() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new InternalAuthenticationServiceException("Invalid credentials"));

        InternalAuthenticationServiceException exception = assertThrows(InternalAuthenticationServiceException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(jwtProperties);

        verify(jwtService, never()).generate(any(User.class), any(Duration.class));
        verify(hashService, never()).hash(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowDisabledExceptionWhenUserNotVerified() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new DisabledException("User was disabled"));

        DisabledException exception = assertThrows(DisabledException.class, () -> authService.login(request));

        assertEquals("User was disabled", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(jwtProperties);

        verify(jwtService, never()).generate(any(User.class), any(Duration.class));
        verify(hashService, never()).hash(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowLockedExceptionWhenUserIsLocked() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new LockedException("User account is locked"));

        LockedException exception = assertThrows(LockedException.class, () -> authService.login(request));

        assertEquals("User account is locked", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(jwtProperties);

        verify(jwtService, never()).generate(any(User.class), any(Duration.class));
        verify(hashService, never()).hash(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenIdentifierOrPasswordIsWrong() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new BadCredentialsException("Invalid credentials"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(jwtProperties);

        verify(jwtService, never()).generate(any(User.class), any(Duration.class));
        verify(hashService, never()).hash(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnAuthenticationResponseWhenLoginIsSuccess() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        User user = User.builder().build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        Duration jwtExpiration = Duration.ofMinutes(5);
        Duration jwtRefreshTokenExpiration = Duration.ofDays(1);

        String refreshToken = "refresh-token";

        when(jwtProperties.expiration()).thenReturn(jwtExpiration);
        when(jwtProperties.refreshTokenExpiration()).thenReturn(jwtRefreshTokenExpiration);
        when(jwtService.generate(user, "access-token", jwtExpiration)).thenReturn("token");
        when(jwtService.generate(user, "refresh-token", jwtRefreshTokenExpiration)).thenReturn(refreshToken);
        when(hashService.hash(refreshToken)).thenReturn("hashed-refresh-token");

        AuthenticationResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtProperties).expiration();
        verify(jwtProperties).refreshTokenExpiration();
        verify(jwtService, times(2)).generate(any(User.class), anyString(), any(Duration.class));
        verify(hashService).hash(refreshToken);
    }


    /**
     * Refresh Token Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenTokenTypeIsInvalid() {
        String refreshToken = "refresh-token";

        when(jwtService.getType(refreshToken)).thenReturn("access-token");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.refresh(refreshToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Token type is invalid", exception.getReason());

        verify(jwtService).getType(refreshToken);

        verifyNoMoreInteractions(
                jwtService
        );

        verifyNoInteractions(
                userRepository,
                hashService,
                jwtProperties
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenRefreshTokenNotFound() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.refresh(refreshToken));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Refresh token not found", exception.getReason());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verifyNoInteractions(
                hashService,
                jwtProperties
        );

        verifyNoMoreInteractions(
                jwtService,
                userRepository
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenRefreshTokenIsInvalid() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        User user = User.builder()
                .refreshToken("saved-refresh-token")
                .build();

        String hashedRefreshToken = "hashed-refresh-token";

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(hashService.hash(refreshToken)).thenReturn(hashedRefreshToken);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.refresh(refreshToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Refresh token is invalid", exception.getReason());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);
        verify(hashService).hash(refreshToken);

        verifyNoInteractions(
                jwtProperties
        );

        verifyNoMoreInteractions(
                jwtService,
                userRepository
        );
    }

    @Test
    void shouldReturnAuthenticationResponseWhenRefreshIsSuccess() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        User user = User.builder()
                .refreshToken("hashed-refresh-token")
                .build();

        String hashedRefreshToken = "hashed-refresh-token";

        Duration jwtExpiration = Duration.ofMinutes(5);
        Duration jwtRefreshTokenExpiration = Duration.ofDays(1);

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(hashService.hash(refreshToken)).thenReturn(hashedRefreshToken);
        when(jwtProperties.expiration()).thenReturn(jwtExpiration);
        when(jwtProperties.refreshTokenExpiration()).thenReturn(jwtRefreshTokenExpiration);
        when(jwtService.generate(user, "access-token", jwtExpiration)).thenReturn("new-access-token");
        when(jwtService.generate(user, "refresh-token", jwtRefreshTokenExpiration)).thenReturn("new-refresh-token");
        when(hashService.hash("new-refresh-token")).thenReturn("hashed-new-refresh-token");

        AuthenticationResponse response = authService.refresh(refreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(user.getEmail(), response.user().email());
        assertEquals(user.getUsername(), response.user().username());
        assertNull(response.user().name());

        assertNotNull(user.getRefreshToken());
        assertNotNull(user.getRefreshTokenExpiredAt());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);
        verify(hashService).hash(refreshToken);
        verify(jwtProperties).expiration();
        verify(jwtProperties).refreshTokenExpiration();
        verify(jwtService, times(2)).generate(any(User.class), anyString(), any(Duration.class));
        verify(userRepository).save(any(User.class));
    }

    /**
     * Logout Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenLogoutTokenTypeIsInvalid() {
        String refreshToken = "refresh-token";

        when(jwtService.getType(refreshToken)).thenReturn("access-token");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.logout(refreshToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Token type is invalid", exception.getReason());

        verify(jwtService).getType(refreshToken);

        verifyNoMoreInteractions(
                jwtService
        );

        verifyNoInteractions(
                userRepository,
                hashService
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenLogoutRefreshTokenNotFound() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.logout(refreshToken));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Refresh token not found", exception.getReason());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verifyNoMoreInteractions(
                userRepository
        );

        verifyNoInteractions(
                hashService
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenLogoutRefreshTokenIsInvalid() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        User user = User.builder()
                .refreshToken("saved-refresh-token")
                .build();

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(hashService.hash(refreshToken)).thenReturn("hashed-refresh-token");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.logout(refreshToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Refresh token is invalid", exception.getReason());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);
        verify(hashService).hash(refreshToken);

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenLogoutIsSuccess() {
        String refreshToken = "refresh-token";

        String userId = "user-x";

        User user = User.builder()
                .refreshToken("hashed-refresh-token")
                .build();

        when(jwtService.getType(refreshToken)).thenReturn("refresh-token");
        when(jwtService.getSubject(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(hashService.hash(refreshToken)).thenReturn("hashed-refresh-token");

        authService.logout(refreshToken);

        assertNull(user.getRefreshToken());
        assertNull(user.getRefreshTokenExpiredAt());

        verify(jwtService).getType(refreshToken);
        verify(jwtService).getSubject(refreshToken);
        verify(userRepository).findById(userId);
        verify(hashService).hash(refreshToken);
        verify(userRepository).save(user);
    }


    /**
     * Forgot Password Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenForgotPasswordUserNotFound() {
        String identifier = "example";

        when(userRepository.findByEmailOrUsername(identifier, identifier)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.forgotPassword(identifier));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());

        verify(userRepository).findByEmailOrUsername(identifier, identifier);

        verifyNoInteractions(
                tokenGenerator,
                hashService,
                mailService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenForgotPasswordIsSuccess() throws MessagingException {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        String identifier = "example";

        String passwordResetToken = "password-reset-token";
        String hashedPasswordResetToken = "hashed-password-reset-token";

        when(userRepository.findByEmailOrUsername(identifier, identifier)).thenReturn(Optional.of(user));
        when(tokenGenerator.generate(anyInt())).thenReturn(passwordResetToken);
        when(hashService.hash(passwordResetToken)).thenReturn(hashedPasswordResetToken);

        authService.forgotPassword(identifier);

        assertNotNull(user.getPasswordResetToken());
        assertNotNull(user.getPasswordResetTokenExpiredAt());

        verify(userRepository).findByEmailOrUsername(identifier, identifier);
        verify(tokenGenerator).generate(anyInt());
        verify(hashService).hash(passwordResetToken);
        verify(userRepository).save(user);
        verify(mailService).sendResetPassword(user.getEmail(), user.getUsername(), passwordResetToken);
    }


    /**
     * Reset Password Test
     */
    @Test
    void shouldThrowResponseStatusExceptionWhenPasswordResetTokenIsInvalid() {
        String passwordResetToken = "password-reset-token";
        String hashedPasswordResetToken = "hashed-password-reset-token";

        when(hashService.hash(passwordResetToken)).thenReturn(hashedPasswordResetToken);
        when(userRepository.findByPasswordResetToken(hashedPasswordResetToken)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.resetPassword(passwordResetToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Token is invalid", exception.getReason());

        verify(hashService).hash(passwordResetToken);
        verify(userRepository).findByPasswordResetToken(hashedPasswordResetToken);

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldThrowResponseStatusExceptionWhenPasswordResetTokenWasExpired() {
        String passwordResetToken = "password-reset-token";
        String hashedPasswordResetToken = "hashed-password-reset-token";

        User user = User.builder()
                .passwordResetTokenExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(hashService.hash(passwordResetToken)).thenReturn(hashedPasswordResetToken);
        when(userRepository.findByPasswordResetToken(hashedPasswordResetToken)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.resetPassword(passwordResetToken));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Token was expired"));

        verify(hashService).hash(passwordResetToken);
        verify(userRepository).findByPasswordResetToken(hashedPasswordResetToken);

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenResetPasswordIsSuccess() {
        String passwordResetToken = "password-reset-token";
        String hashedPasswordResetToken = "hashed-password-reset-token";

        User user = User.builder()
                .passwordResetTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(hashService.hash(passwordResetToken)).thenReturn(hashedPasswordResetToken);
        when(userRepository.findByPasswordResetToken(hashedPasswordResetToken)).thenReturn(Optional.of(user));

        authService.resetPassword(passwordResetToken);

        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetTokenExpiredAt());

        verify(hashService).hash(passwordResetToken);
        verify(userRepository).findByPasswordResetToken(hashedPasswordResetToken);
        verify(userRepository).save(user);
    }
}