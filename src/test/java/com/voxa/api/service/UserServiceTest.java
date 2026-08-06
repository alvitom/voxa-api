package com.voxa.api.service;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.entity.UserProfile;
import com.voxa.api.model.projection.UserProjection;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProjection userProjection;

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


    @Test
    void shouldReturnVoidWhenDeleteUserIsSuccess() {

    }
}