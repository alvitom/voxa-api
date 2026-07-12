package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowConflictWhenUsernameAlreadyExists() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "081234567890",
                "example",
                "password"
        );

        // When
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        // Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository).existsByUsername(request.username());

        verifyNoInteractions(
                passwordEncoder,
                jwtService
        );

        verifyNoMoreInteractions(
                userRepository
        );
    }

    @Test
    void shouldRegisterSuccessfully() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "081234567890",
                "example",
                "password"
        );

        User user = User.builder()
                .id("USER-001")
                .email("john@example.com")
                .phoneNumber("081234567890")
                .username("example")
                .password("hashed-password")
                .build();

        // When
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generate(user)).thenReturn("token");

        // Then
        AuthenticationResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("token", response.token());

        verify(userRepository).existsByUsername(request.username());
        verify(passwordEncoder).encode(request.password());

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userArgumentCaptor.capture());

        User userArgumentCaptorValue = userArgumentCaptor.getValue();

        assertEquals("hashed-password", userArgumentCaptorValue.getPassword());

        verify(jwtService).generate(user);
    }
}