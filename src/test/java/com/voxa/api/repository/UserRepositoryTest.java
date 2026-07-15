package com.voxa.api.repository;

import com.voxa.api.config.DatabaseConfig;
import com.voxa.api.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(DatabaseConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldThrowExceptionWhenUsernameIsNull() {
        // When
        User user = User.builder()
                .email("john@example.com")
                .password("example")
                .build();

        // Then
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user));
    }

    @Test
    void shouldThrowExceptionWhenUsernameOrEmailAlreadyExists() {
        // Given
        User user1 = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();
        userRepository.saveAndFlush(user1);

        // When
        User user2 = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        // Then
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
    }

    @Test
    void shouldFindUserByUsername() {
        // Given
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();
        userRepository.save(user);

        // When
        boolean findUser = userRepository.existsByUsername("example");

        // Then
        assertTrue(findUser);
    }
}