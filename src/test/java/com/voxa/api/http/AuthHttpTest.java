package com.voxa.api.http;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RefreshTokenUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.request.VerificationAccountUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.service.HashService;
import com.voxa.api.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
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
    @Qualifier("hs256")
    private HashService hs256Service;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    @Qualifier("sha256")
    private HashService sha256Service;

    @Autowired
    private JwtService jwtService;

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
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
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
                .verificationToken(hs256Service.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        VerificationAccountUserRequest request = new VerificationAccountUserRequest("token");

        webTestClient.post()
                .uri("/v1/auth/verify-account")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                });
    }

    @Test
    void shouldLoginSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .verificationToken(hs256Service.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("john@example.com", "password");

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("User login successfully", webResponse.message());
                });
    }

    @Test
    void shouldRefreshSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        User savedUser = userRepository.save(user);

        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String refreshToken = jwtService.generate(savedUser, "refresh-token", refreshTokenExpiration);

        savedUser.setRefreshToken(sha256Service.hash(refreshToken));
        savedUser.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(savedUser);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        webTestClient.post()
                .uri("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("Refresh token successfully", webResponse.message());
                });
    }

    @Test
    void shouldLogoutSuccessfully() {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        User savedUser = userRepository.save(user);

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String accessToken = jwtService.generate(savedUser, "access-token", accessTokenExpiration);
        String refreshToken = jwtService.generate(savedUser, "refresh-token", refreshTokenExpiration);

        savedUser.setRefreshToken(sha256Service.hash(refreshToken));
        savedUser.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(savedUser);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        webTestClient.post()
                .uri("/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(new ParameterizedTypeReference<WebResponse<AuthenticationResponse>>() {
                })
                .value(webResponse -> {
                    assertTrue(webResponse.success());
                    assertEquals("Logout successfully", webResponse.message());
                });
    }
}
