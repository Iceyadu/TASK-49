package com.scholarops.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.LoginRequest;
import com.scholarops.model.entity.User;
import com.scholarops.security.UserDetailsImpl;
import com.scholarops.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void testFullLoginFlow() throws Exception {
        User user = User.builder()
                .id(1L).username("admin").email("admin@test.com")
                .passwordHash("encoded-password")
                .enabled(true).accountLocked(false).build();

        UserDetailsImpl userDetails = new UserDetailsImpl(user,
                Set.of("ADMINISTRATOR"), Set.of("USER_MANAGE", "ROLE_ASSIGN", "AUDIT_VIEW"));

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest loginRequest = new LoginRequest("admin", "StrongPass1!");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }
}
