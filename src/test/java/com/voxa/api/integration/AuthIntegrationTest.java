package com.voxa.api.integration;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.*;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.ErrorResponse;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("hs256")
    private HashService hs256Service;

    @Autowired
    @Qualifier("sha256")
    private HashService sha256Service;

    private String basePath = "/v1/auth";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }


    /**
     * Register Test
     * @throws Exception
     */
    @Test
    void shouldReturnErrorBadRequestWhenRegisterUserRequestIsInvalid() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "",
                "",
                "",
                ""
        );

        mockMvc.perform(
                post(basePath + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Validation error"));
                    assertEquals(6, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorConflictWhenEmailAlreadyExists() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        userRepository.save(user);

        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        mockMvc.perform(
                post(basePath + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.CONFLICT.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Email already exists"));
                }
        );
    }

    @Test
    void shouldReturnErrorConflictWhenUsernameAlreadyExists() throws Exception {
        User user = User.builder()
                .email("doe@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        userRepository.save(user);

        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        mockMvc.perform(
                post(basePath + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.CONFLICT.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Username already exists"));
                }
        );
    }

    @Test
    void shouldReturnSuccessCreatedWhenRegisterIsSuccess() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "john@example.com",
                "example",
                "password",
                "password"
        );

        mockMvc.perform(
                post(basePath + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.CREATED.value(), result.getResponse().getStatus());

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Registration successful. Please check your email.", webResponse.message());
                    assertNull(webResponse.data());
                }
        );
    }


    /**
     * Verify Account Test
     * @throws Exception
     */
    @Test
    void shouldReturnErrorBadRequestWhenVerifyAccountTokenIsBlank() throws Exception {
        VerificationAccountUserRequest request = new VerificationAccountUserRequest("");

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(1, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenVerifyAccountTokenIsInvalid() throws Exception {
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

        VerificationAccountUserRequest request = new VerificationAccountUserRequest("invalid-token");

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Token is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenVerifyAccountTokenWasExpired() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(false)
                .verificationToken(hs256Service.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        userRepository.save(user);

        VerificationAccountUserRequest request = new VerificationAccountUserRequest("token");

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Token was expired"));
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenVerifyAccountUserAlreadyVerified() throws Exception {
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

        VerificationAccountUserRequest request = new VerificationAccountUserRequest("token");

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("User already verified.", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenVerifyAccountIsSuccess() throws Exception {
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

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                    assertNotNull(webResponse.data().accessToken());
                    assertNotNull(webResponse.data().refreshToken());
                    assertEquals("john@example.com", webResponse.data().user().email());
                    assertEquals("example", webResponse.data().user().username());
                }
        );
    }


    /**
     * Login Test
     * @throws Exception
     */
    @Test
    void shouldReturnErrorBadRequestWhenLoginUserRequestIsInvalid() throws Exception {
        LoginUserRequest request = new LoginUserRequest("", "");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(2, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorUnauthorizedWhenUserNotFound() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("not-found", "example");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Invalid credentials", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorUnauthorizedWhenUserNotVerified() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(false)
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("example", "example");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Your account not verified", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorUnauthorizedWhenUserIsLocked() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(false)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("example", "password");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Your account is locked"));
                }
        );
    }

    @Test
    void shouldReturnErrorUnauthorizedWhenIdentifierOrPasswordIsWrong() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("example", "example");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Invalid credentials", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenLoginIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .build();

        userRepository.save(user);

        LoginUserRequest request = new LoginUserRequest("example", "password");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

                    String responseBody = result.getResponse().getContentAsString();

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("User login successfully", webResponse.message());

                    AuthenticationResponse authenticationResponse = webResponse.data();

                    assertNotNull(authenticationResponse.accessToken());
                    assertNotNull(authenticationResponse.refreshToken());
                    assertEquals("john@example.com", authenticationResponse.user().email());
                    assertEquals("example", authenticationResponse.user().username());
                }
        );
    }


    /**
     * Refresh Token Test
     */
    @Test
    void shouldReturnErrorBadRequestWhenRefreshTokenIsBlank() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("");

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(1, errorResponse.errors().size());
                }
        );
    }

    // TODO: Implement JWT Service Test to separate logic test

//    @Test
//    void shouldReturnErrorUnauthorizedWhenRefreshTokenWasExpired() throws Exception {
//        User user = User.builder()
//                .build();
//
//        Duration refreshTokenExpiration = Duration.ofDays(1).minusDays(2);
//
//        String refreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);
//
//        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);
//
//        mockMvc.perform(
//                post(basePath + "/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//        ).andExpectAll(
//                status().isUnauthorized(),
//                content().contentType(MediaType.APPLICATION_JSON),
//                result -> {
//                    String responseBody = result.getResponse().getContentAsString();
//
//                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
//                    });
//
//                    assertFalse(errorResponse.success());
//                    assertTrue(errorResponse.message().contains("JWT expired"));
//                }
//        );
//    }

    @Test
    void shouldReturnErrorForbiddenWhenTokenTypeWasIsInvalid() throws Exception {
        User user = User.builder()
                .build();

        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String refreshToken = jwtService.generate(user, "access-token", refreshTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Token type is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorNotFoundWhenRefreshTokenNotFound() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .build();

        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String refreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Refresh token not found", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenRefreshTokenIsInvalid() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .refreshToken("refresh-token")
                .build();

        User savedUser = userRepository.save(user);

        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String refreshToken = jwtService.generate(savedUser, "refresh-token", refreshTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Refresh token is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenRefreshIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        userRepository.save(user);

        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String refreshToken = jwtService.generate(user, "refresh-token", refreshTokenExpiration);

        String hashedRefreshToken = sha256Service.hash(refreshToken);

        user.setRefreshToken(hashedRefreshToken);
        user.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(user);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Refresh token successfully", webResponse.message());
                }
        );
    }


    /**
     * Logout Test
     */
    @Test
    void shouldReturnErrorUnauthorizedWhenAccessTokenNotPresent() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("");

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Full authentication is required"));
                }
        );
    }

    @Test
    void shouldReturnErrorBadRequestWhenLogoutRefreshTokenIsBlank() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        User savedUser = userRepository.save(user);

        Duration accessTokenExpiration = jwtProperties.expiration();

        String accessToken = jwtService.generate(savedUser, "access-token", accessTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest("");

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(1, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenLogoutRefreshTokenTypeIsInvalid() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        User savedUser = userRepository.save(user);

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String accessToken = jwtService.generate(savedUser, "access-token", accessTokenExpiration);
        String refreshToken = jwtService.generate(savedUser, "invalid-token", refreshTokenExpiration);

        savedUser.setRefreshToken(sha256Service.hash(refreshToken));
        savedUser.setRefreshTokenExpiredAt(LocalDateTime.now().plus(refreshTokenExpiration));

        userRepository.save(savedUser);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Token type is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorNotFoundWhenLogoutRefreshTokenNotFound() throws Exception {
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

        User user2 = User.builder()
                .id(UUID.randomUUID().toString())
                .build();

        String notFoundToken = jwtService.generate(user2, "refresh-token", refreshTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(notFoundToken);

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Refresh token not found", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenLogoutRefreshTokenIsInvalid() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .refreshToken("refresh-token")
                .build();

        User savedUser = userRepository.save(user);

        Duration accessTokenExpiration = jwtProperties.expiration();
        Duration refreshTokenExpiration = jwtProperties.refreshTokenExpiration();

        String accessToken = jwtService.generate(savedUser, "access-token", accessTokenExpiration);
        String refreshToken = jwtService.generate(savedUser, "refresh-token", refreshTokenExpiration);

        RefreshTokenUserRequest request = new RefreshTokenUserRequest(refreshToken);

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Refresh token is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenLogoutIsSuccess() throws Exception {
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

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    WebResponse<?> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Logout successfully", webResponse.message());
                }
        );
    }


    /**
     * Forgot Password Test
     */
    @Test
    void shouldReturnErrorBadRequestWhenForgotPasswordIdentifierIsBlank() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("");

        mockMvc.perform(
                post(basePath + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(1, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorNotFoundWhenForgotPasswordUserDoesNotExists() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("example");

        mockMvc.perform(
                post(basePath + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("User not found", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenForgotPasswordIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        userRepository.save(user);

        ForgotPasswordRequest request = new ForgotPasswordRequest("example");

        mockMvc.perform(
                post(basePath + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    WebResponse<?> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertTrue(webResponse.message().contains("Reset password request successful"));
                }
        );
    }


    /**
     * Reset Password Test
     */
    @Test
    void shouldReturnErrorBadRequestWhenResetPasswordTokenIsBlank() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("");

        mockMvc.perform(
                post(basePath + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                    assertNotNull(errorResponse.errors());
                    assertEquals(1, errorResponse.errors().size());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenResetPasswordTokenIsInvalid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token");

        mockMvc.perform(
                post(basePath + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Token is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnErrorForbiddenWhenResetPasswordTokenWasExpired() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .passwordResetToken(hs256Service.hash("token"))
                .passwordResetTokenExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        userRepository.save(user);

        ResetPasswordRequest request = new ResetPasswordRequest("token");

        mockMvc.perform(
                post(basePath + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isForbidden(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Token was expired"));
                }
        );
    }

    @Test
    void shouldReturnSuccessOkWhenResetPasswordIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .passwordResetToken(hs256Service.hash("token"))
                .passwordResetTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        ResetPasswordRequest request = new ResetPasswordRequest("token");

        mockMvc.perform(
                post(basePath + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String responseBody = result.getResponse().getContentAsString();

                    WebResponse<?> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertTrue(webResponse.message().contains("Reset password successfully"));
                }
        );
    }
}
