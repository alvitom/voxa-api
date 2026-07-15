package com.voxa.api.http;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.service.HashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AuthHttpTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HashService hashService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterSuccessfully() {
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        webTestClient.post()
                .uri("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("Registration successful. Please check your email.", webResponse.message());
                });
    }

    @Test
    void shouldVerifyAccountSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(false)
                .verificationToken(hashService.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        String token = "token";

        webTestClient.post()
                .uri("/v1/auth/verify-account?token={token}", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                });
    }
}
