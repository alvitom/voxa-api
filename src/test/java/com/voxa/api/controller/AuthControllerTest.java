package com.voxa.api.controller;

import com.voxa.api.model.request.*;
import com.voxa.api.model.response.ErrorResponse;
import com.voxa.api.model.response.AuthenticationResponse;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private String basePath = "/v1/auth";

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Register Test
     * @throws Exception
     */
    @Test
    void shouldThrowMethodArgumentNotValidExceptionWhenRegisterUserRequestIsInvalid() throws Exception {
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
        ).andExpect(status().isBadRequest()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertFalse(errorResponse.success());
            assertEquals("Validation error", errorResponse.message());
        });

        verify(authService, never()).register(request);
    }

    @Test
    void shouldReturnWebResponseWhenRegisterIsSuccess() throws Exception {
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
        ).andExpect(status().isCreated()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("Registration successful. Please check your email.", webResponse.message());
        });

        verify(authService).register(request);
    }

    /**
     * Verify Account Test
     * @throws Exception
     */
    @Test
    void shouldThrowConstraintViolationExceptionWhenVerifyAccountTokenIsBlank() throws Exception {
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

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                }
        );

        verify(authService, never()).verifyAccount(request.verificationToken());
    }

    @Test
    void shouldReturnWebResponseWhenVerifyAccountIsSuccess() throws Exception {
        VerificationAccountUserRequest request = new VerificationAccountUserRequest("verification-token");

        UserResponse userResponse = UserResponse.builder()
                .email("john@example.com")
                .username("example")
                .build();

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(userResponse)
                .build();

        when(authService.verifyAccount(request.verificationToken())).thenReturn(authenticationResponse);

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                    assertEquals("access-token", webResponse.data().accessToken());
                    assertEquals("refresh-token", webResponse.data().refreshToken());
                    assertEquals("john@example.com", webResponse.data().user().email());
                    assertEquals("example", webResponse.data().user().username());
                }
        );

        verify(authService).verifyAccount(request.verificationToken());
    }


    /**
     * Resend Account Verification Test
     */


    /**
     * Login Test
     * @throws Exception
     */
    @Test
    void shouldThrowMethodArgumentNotValidExceptionWhenLoginUserRequestIsInvalid() throws Exception {
        LoginUserRequest request = new LoginUserRequest("", "");

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            ErrorResponse errorResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertFalse(errorResponse.success());
            assertEquals("Validation error", errorResponse.message());
        });

        verify(authService, never()).login(request);
    }

    @Test
    void shouldReturnWebResponseWhenLoginIsSuccess() throws Exception {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.login(request)).thenReturn(authenticationResponse);

        mockMvc.perform(
                post(basePath + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("User login successfully", webResponse.message());

            String accessToken = webResponse.data().accessToken();
            String refreshToken = webResponse.data().refreshToken();

            assertEquals("access-token", accessToken);
            assertEquals("refresh-token", refreshToken);
        });

        verify(authService).login(request);
    }


    /**
     * Refresh Token Test
     */
    @Test
    void shouldThrowConstraintViolationExceptionWhenRefreshTokenIsBlank() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("");

        mockMvc.perform(
                post(basePath + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                }
        );

        verify(authService, never()).refresh(request.refreshToken());
    }

    @Test
    void shouldReturnWebResponseWhenRefreshIsSuccess() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("refresh-token");

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.refresh(request.refreshToken())).thenReturn(authenticationResponse);

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

                    String newAccesstoken = webResponse.data().accessToken();
                    String newRefreshToken = webResponse.data().refreshToken();

                    assertEquals("access-token", newAccesstoken);
                    assertEquals("refresh-token", newRefreshToken);
                }
        );

        verify(authService).refresh(request.refreshToken());
    }


    /**
     * Logout Test
     * @throws Exception
     */
    @Test
    void shouldThrowConstraintViolationExceptionWhenLogoutRefreshTokenIsBlank() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("");

        mockMvc.perform(
                post(basePath + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                }
        );

        verify(authService, never()).logout(request.refreshToken());
    }

    @Test
    void shouldReturnWebResponseWhenLogoutIsSuccess() throws Exception {
        RefreshTokenUserRequest request = new RefreshTokenUserRequest("refresh-token");

        mockMvc.perform(
                post(basePath + "/logout")
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
                    assertEquals("Logout successfully", webResponse.message());
                }
        );

        verify(authService).logout(request.refreshToken());
    }


    /**
     * Forgot Password Test
     */
    @Test
    void shouldThrowMethodArgumentNotValidExceptionWhenForgotPasswordRequestIsInvalid() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("");

        mockMvc.perform(
                post(basePath + "/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                }
        );

        verify(authService, never()).forgotPassword(request.identifier());
    }

    @Test
    void shouldReturnWebResponseWhenForgotPasswordIsSuccess() throws Exception {
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

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertTrue(webResponse.message().contains("Reset password request successful"));
                }
        );

        verify(authService).forgotPassword(request.identifier());
    }


    /**
     * Reset Password Test
     */
    @Test
    void shouldThrowMethodArgumentNotValidExceptionWhenResetPasswordRequestIsInvalid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("");

        mockMvc.perform(
                post(basePath + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    ErrorResponse errorResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertFalse(errorResponse.success());
                    assertEquals("Validation error", errorResponse.message());
                }
        );

        verify(authService, never()).resetPassword(request.passwordResetToken());
    }

    @Test
    void shouldReturnWebResponseWhenResetPasswordIsSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("password-reset-token");

        mockMvc.perform(
                post(basePath + "/reset-password")
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
                    assertTrue(webResponse.message().contains("Reset password successfully"));
                }
        );

        verify(authService).resetPassword(request.passwordResetToken());
    }
}