package com.voxa.api.controller;

import com.voxa.api.model.entity.User;
import com.voxa.api.model.request.UpdateUserRequest;
import com.voxa.api.model.response.UserResponse;
import com.voxa.api.model.response.WebResponse;
import com.voxa.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private String basePath = "/v1/users";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnWebResponseWhenGetCurrentUserIsSuccess() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .email("john@example.com")
                .username("example")
                .name("John Doe")
                .build();

        User user = User.builder()
                .id("id")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userService.get(user.getId())).thenReturn(userResponse);

        mockMvc.perform(
                get(basePath + "/me")
        ).andExpect(status().isOk()).andExpect(result -> {
            String responseBody = result.getResponse().getContentAsString();

            WebResponse<UserResponse> webResponse = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            assertTrue(webResponse.success());
            assertEquals("Get current user successfully", webResponse.message());
            assertNotNull(webResponse.data());
        });

        verify(userService).get(user.getId());
    }

    @Test
    void shouldReturnWebResponseWhenUpdateUserIsSuccess() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "John Doe",
                "Bio test",
                "Profile Picture URL test"
        );

        UserResponse userResponse = UserResponse.builder()
                .email("john@example.com")
                .username("example")
                .name("John Doe")
                .build();

        User user = User.builder()
                .id("id")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userService.update(user.getId(), request)).thenReturn(userResponse);

        mockMvc.perform(
                patch(basePath + "/me")
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

        verify(userService).update(user.getId(), request);
    }
}