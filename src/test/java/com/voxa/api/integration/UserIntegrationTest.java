package com.voxa.api.integration;

import com.voxa.api.config.JwtProperties;
import com.voxa.api.model.entity.Gender;
import com.voxa.api.model.entity.User;
import com.voxa.api.model.entity.UserProfile;
import com.voxa.api.model.request.DeleteUserRequest;
import com.voxa.api.model.request.DeleteVerificationRequest;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.ErrorResponse;
import com.voxa.api.model.response.UserResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private String basePath = "/v1/users";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("hs256")
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
    void shouldReturnErrorUnauthorizedWhenGetCurrentUserAccessTokenNotPresent() throws Exception {
        mockMvc.perform(
                get(basePath + "/me")
        ).andExpect(status().isUnauthorized()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertFalse(errorResponse.success());
            assertTrue(errorResponse.message().contains("Full authentication is required"));
        });
    }

    @Test
    void shouldReturnSuccessOkWhenGetCurrentUserIsSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .name("John Doe")
                .birthday(LocalDate.now())
                .gender(Gender.MALE)
                .build();

        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        user.setProfile(profile);
        profile.setUser(user);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generate(savedUser, "access-token", jwtProperties.expiration());

        mockMvc.perform(
                get(basePath + "/me")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<UserResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("Get current user successfully", webResponse.message());
            assertNotNull(webResponse.data());
        });
    }

    @Test
    void shouldReturnSuccessOkWhenUpdateUserIsSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .name("John Doe")
                .birthday(LocalDate.now())
                .gender(Gender.MALE)
                .build();

        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password("password")
                .build();

        user.setProfile(profile);
        profile.setUser(user);

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generate(savedUser, "access-token", jwtProperties.expiration());

        UpdateUserRequest request = new UpdateUserRequest(
                "John Doe",
                "Bio test",
                "Profile Picture URL test"
        );

        mockMvc.perform(
                patch(basePath + "/me")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<UserResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("Update user successfully", webResponse.message());
            assertNotNull(webResponse.data());
        });
    }

    @Test
    void shouldReturnSuccessOkWhenSendDeleteVerificationIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generate(savedUser, "access-token", jwtProperties.expiration());

        DeleteVerificationRequest request = new DeleteVerificationRequest(
                "password"
        );

        mockMvc.perform(
                post(basePath + "/me/send-delete-verification")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<UserResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertTrue(webResponse.message().contains("Delete verification request successfully"));
            assertNull(webResponse.data());
        });
    }

    @Test
    void shouldReturnSuccessOkWhenDeleteIsSuccess() throws Exception {
        User user = User.builder()
                .email("john@example.com")
                .username("example")
                .password(passwordEncoder.encode("password"))
                .deleteVerificationCode(hashService.hash("delete-verification"))
                .deleteVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generate(savedUser, "access-token", jwtProperties.expiration());

        DeleteUserRequest request = new DeleteUserRequest(
                "delete-verification"
        );

        mockMvc.perform(
                delete(basePath + "/me")
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<UserResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertTrue(webResponse.message().contains("Delete user successfully"));
            assertNull(webResponse.data());
        });
    }
}
