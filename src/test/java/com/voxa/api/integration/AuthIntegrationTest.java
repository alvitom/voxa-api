package com.voxa.api.integration;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.ErrorResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.repository.UserRepository;
import com.voxa.api.service.HashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

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
    private HashService hashService;

    private String basePath = "/v1/auth";

    @Autowired
    private ObjectMapper objectMapper;

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
    void shouldReturnErrorForbiddenWhenVerifyAccountTokenIsInvalid() throws Exception {
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

        String requestParam = "invalid-token";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", requestParam)
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
                .verificationToken(hashService.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        userRepository.save(user);

        String requestParam = "token";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", requestParam)
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
                .verificationToken(hashService.hash("token"))
                .verificationTokenExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        userRepository.save(user);

        String requestParam = "token";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", requestParam)
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
    void shouldReturnErrorBadRequestWhenVerifyAccountTokenIsBlank() throws Exception {
        String requestParam = "";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", requestParam)
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
    void shouldReturnSuccessOkWhenVerifyAccountIsSuccess() throws Exception {
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

        String requestParam = "token";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", requestParam)
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
                    assertNotNull(webResponse.data().token());
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

                    assertNotNull(authenticationResponse.token());
                    assertEquals("john@example.com", authenticationResponse.user().email());
                    assertEquals("example", authenticationResponse.user().username());
                }
        );
    }
}
