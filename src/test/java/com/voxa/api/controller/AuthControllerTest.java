package com.voxa.api.controller;

import com.voxa.api.model.request.RegisterUserRequest;
import com.voxa.api.model.response.ErrorResponse;
import com.voxa.api.model.response.AuthenticationResponse;
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
                "081234567890",
                "example",
                "password"
        );

        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token("token")
                .build();

        when(authService.register(request)).thenReturn(authenticationResponse);

        mockMvc.perform(
                post(basePath + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<AuthenticationResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("User registered successfully", webResponse.message());

            String token = webResponse.data().token();

            assertEquals("token", token);
        });

        verify(authService).register(request);
    }
}