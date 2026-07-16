package com.voxa.api.service;

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

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

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

        verifyNoInteractions(jwtService);
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

        verifyNoInteractions(jwtService);
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

        verifyNoInteractions(jwtService);
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

        when(hashService.hash(verificationToken)).thenReturn(hashedVerificationToken);
        when(userRepository.findByVerificationToken(hashedVerificationToken)).thenReturn(Optional.of(user));
        when(jwtService.generate(user)).thenReturn("token");

        AuthenticationResponse authenticationResponse = authService.verifyAccount(verificationToken);

        assertNotNull(authenticationResponse.token());
        assertEquals(user.getEmail(), authenticationResponse.user().email());
        assertEquals(user.getUsername(), authenticationResponse.user().username());
        assertNull(authenticationResponse.user().name());

        assertTrue(user.isEnabled());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiredAt());

        verify(hashService).hash(verificationToken);
        verify(userRepository).findByVerificationToken(hashedVerificationToken);
        verify(userRepository).save(user);
        verify(jwtService).generate(user);
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
        verify(authentication, never()).getPrincipal();
        verify(jwtService, never()).generate(any(User.class));
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
        verify(authentication, never()).getPrincipal();
        verify(jwtService, never()).generate(any(User.class));
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
        verify(authentication, never()).getPrincipal();
        verify(jwtService, never()).generate(any(User.class));
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
        verify(authentication, never()).getPrincipal();
        verify(jwtService, never()).generate(any(User.class));
    }

    @Test
    void shouldReturnAuthenticationResponseWhenLoginIsSuccess() {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        User user = User.builder().build();

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generate(user)).thenReturn("token");

        AuthenticationResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token", response.token());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authentication).getPrincipal();
        verify(jwtService).generate(user);
    }
}