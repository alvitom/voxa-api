package com.voxa.api.controller;

import com.voxa.api.model.request.LoginUserRequest;
import com.voxa.api.model.request.RegisterUserRequest;
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
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthService authService;

    private String basePath = "/v1/auth";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldThrowMethodArgumentNotValidExceptionWhenRequestIsInvalid() throws Exception {
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
        ).andExpect(status().isCreated()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("Registration successful. Please check your email.", webResponse.message());
        });

        verify(authService).register(request);
    }

    @Test
    void shouldReturnBadRequestWhenVerifyAccountTokenIsBlank() throws Exception {
        String token = "";

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", token)
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

        verify(authService, never()).verifyAccount(token);
    }

    @Test
    void shouldReturnOkWhenVerifyAccountIsSuccess() throws Exception {
        String token = "token";

        UserResponse userResponse = UserResponse.builder()
                .email("john@example.com")
                .username("example")
                .build();

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token("token")
                .userResponse(userResponse)
                .build();

        when(authService.verifyAccount(token)).thenReturn(authenticationResponse);

        mockMvc.perform(
                post(basePath + "/verify-account")
                        .queryParam("token", token)
        ).andExpectAll(
                status().isOk(),
                content().contentType(MediaType.APPLICATION_JSON),
                result -> {
                    String response = result.getResponse().getContentAsString();

                    WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(response, new TypeReference<>() {
                    });

                    assertTrue(webResponse.success());
                    assertEquals("Account verified successfully", webResponse.message());
                    assertEquals("token", webResponse.data().token());
                    assertEquals("john@example.com", webResponse.data().userResponse().email());
                    assertEquals("example", webResponse.data().userResponse().username());
                }
        );

        verify(authService).verifyAccount(token);
    }

    @Test
    void shouldReturnBadRequestWhenLoginUserRequestBodyIsInvalid() throws Exception {
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
    void shouldReturnOkWhenLoginIsSuccess() throws Exception {
        LoginUserRequest request = new LoginUserRequest("example", "password");

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token("token")
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

            String token = webResponse.data().token();

            assertEquals("token", token);
        });

        verify(authService).login(request);
    }
}