package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.entity.UserProfile;
import com.voxa.api.model.projection.UserProjection;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.util.TokenGenerator;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProjection userProjection;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private HashService hashService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private UserService userService;


    /**
     * Get Current User Test
     */
    @Test
    void shouldReturnUserResponseWhenGetCurrentUserIsSuccess() {
        when(userProjection.getEmail()).thenReturn("john@example.com");
        when(userProjection.getUsername()).thenReturn("example");
        when(userProjection.getName()).thenReturn("John Doe");

        when(userRepository.findCurrentUser(anyString())).thenReturn(Optional.of(userProjection));

        UserResponse userResponse = userService.get(anyString());

        assertEquals("john@example.com", userResponse.email());
        assertEquals("example", userResponse.username());
        assertEquals("John Doe", userResponse.name());

        verify(userRepository).findCurrentUser(anyString());
    }


    /**
     * Update User Test
     */
    @Test
    void shouldReturnUserResponseWhenUpdateUserIsSuccess() {
        UpdateUserRequest request = new UpdateUserRequest(
                "John Doe",
                "Bio test",
                "Profile Picture URL test"
        );

        UserProfile profile = UserProfile.builder()
                .name("John Doe")
                .build();

        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .profile(profile)
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse userResponse = userService.update(user.getId(), request);

        assertEquals("john@example.com", userResponse.email());
        assertEquals("example", userResponse.username());
        assertEquals("John Doe", userResponse.name());

        verify(userRepository).save(user);
    }


    /**
     * Send Delete User Verification Test
     */
    @Test
    void shouldThrowExceptionWhenSendDeleteVerificationPasswordIsWrong() {
        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        String password = "password";

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.sendDeleteVerification(user.getId(), password));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Password is wrong", exception.getReason());

        verify(userRepository).findById(anyString());
        verify(passwordEncoder).matches(anyString(), anyString());

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
    void shouldReturnVoidWhenSendDeleteVerificationIsSuccess() throws MessagingException {
        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        String password = "password";

        String verificationCode = "delete-verification";
        String hashedVerificationCode = "hashed-delete-verification";

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(tokenGenerator.generate(anyInt())).thenReturn(verificationCode);
        when(hashService.hash(verificationCode)).thenReturn(hashedVerificationCode);

        userService.sendDeleteVerification(user.getId(), password);

        verify(userRepository).findById(anyString());
        verify(passwordEncoder).matches(anyString(), anyString());
        verify(tokenGenerator).generate(anyInt());
        verify(hashService).hash(verificationCode);
        verify(userRepository).save(user);
        verify(mailService).sendDeleteVerification(user.getEmail(), user.getUsername(), verificationCode);
    }


    /**
     * Delete User Test
     */
    @Test
    void shouldThrowExceptionWhenDeleteVerificationCodeIsInvalid() {
        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .password("password")
                .deleteVerificationCode("hashed-delete-verification")
                .deleteVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(hashService.hash(anyString())).thenReturn("invalid-delete-verification");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.delete("id", anyString()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Invalid verification code", exception.getReason());

        verify(userRepository).findById(anyString());
        verify(hashService).hash(anyString());

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldThrowExceptionWhenDeleteVerificationCodeWasExpired() {
        String hashedVerificationCode = "hashed-delete-verification";

        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .password("password")
                .deleteVerificationCode(hashedVerificationCode)
                .deleteVerificationCodeExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(hashService.hash(anyString())).thenReturn(hashedVerificationCode);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.delete("id", anyString()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Verification code was expired", exception.getReason());

        verify(userRepository).findById(anyString());
        verify(hashService).hash(anyString());

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldReturnVoidWhenDeleteIsSuccess() {
        String hashedVerificationCode = "hashed-delete-verification";

        User user = User.builder()
                .id("id")
                .email("john@example.com")
                .username("example")
                .password("password")
                .deleteVerificationCode(hashedVerificationCode)
                .deleteVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));
        when(hashService.hash(anyString())).thenReturn(hashedVerificationCode);

        userService.delete("id", anyString());

        verify(userRepository).findById(anyString());
        verify(hashService).hash(anyString());
        verify(userRepository).delete(user);
    }
}