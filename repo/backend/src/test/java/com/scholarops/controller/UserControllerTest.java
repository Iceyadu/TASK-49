package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.AdminPasswordResetRequest;
import com.scholarops.model.dto.UserCreateRequest;
import com.scholarops.model.entity.User;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void testListUsersAsAdmin() throws Exception {
        User user = User.builder().id(1L).username("user1")
                .email("user1@test.com").fullName("User One")
                .enabled(true).accountLocked(false).build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userService.listUsers(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testListUsersAsStudent403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void testCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("StrongPass1!xy");
        request.setFullName("New User");

        User created = User.builder().id(1L).username("newuser")
                .email("new@test.com").fullName("New User")
                .enabled(true).accountLocked(false).build();

        when(userService.createUser(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void testAdminPasswordReset() throws Exception {
        AdminPasswordResetRequest request = new AdminPasswordResetRequest();
        request.setNewPassword("NewStrongPass1!");
        request.setWorkstationId("WS-001");
        request.setReason("User locked out");

        mockMvc.perform(post("/api/users/1/admin-reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
