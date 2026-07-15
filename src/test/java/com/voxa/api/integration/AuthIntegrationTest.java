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

    @Test
    void shouldRegisterSuccessfully() throws Exception {
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

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Registration successful. Please check your email.", webResponse.message());
                }
        );
    }

    @Test
    void shouldReturnForbiddenWhenVerifyAccountTokenIsInvalid() throws Exception {
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

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Token is invalid", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnForbiddenWhenVerifyAccountTokenWasExpired() throws Exception {
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

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertTrue(errorResponse.message().contains("Token was expired"));
                }
        );
    }

    @Test
    void shouldReturnForbiddenWhenVerifyAccountUserAlreadyVerified() throws Exception {
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

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("User already verified.", errorResponse.message());
                }
        );
    }

    @Test
    void shouldReturnOkWhenVerifyAccountIsSuccess() throws Exception {
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

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                    assertNotNull(webResponse.data().token());
                    assertEquals("john@example.com", webResponse.data().userResponse().email());
                    assertEquals("example", webResponse.data().userResponse().username());
                }
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginIsFailed() throws Exception {
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

        LoginUserRequest request = new LoginUserRequest("wrong", "example");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    System.out.println(result.getResponse().getContentAsString());
                }
        );
    }
}
