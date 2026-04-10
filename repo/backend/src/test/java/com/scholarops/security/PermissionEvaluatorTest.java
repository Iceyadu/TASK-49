package com.scholarops.security;

import com.scholarops.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionEvaluatorTest {

    private PermissionEvaluatorImpl permissionEvaluator;

    @BeforeEach
    void setUp() {
        permissionEvaluator = new PermissionEvaluatorImpl();
    }

    private Authentication createAuthentication(Set<String> permissions) {
        User user = User.builder().id(1L).username("testuser")
                .email("test@test.com").passwordHash("hash")
                .enabled(true).accountLocked(false).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user,
                Set.of("ADMINISTRATOR"), permissions);
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testHasPermissionGranted() {
        Authentication auth = createAuthentication(Set.of("USER_MANAGE", "ROLE_ASSIGN"));
        assertTrue(permissionEvaluator.hasPermission(auth, null, "USER_MANAGE"));
        assertTrue(permissionEvaluator.hasPermission(auth, null, "ROLE_ASSIGN"));
    }

    @Test
    void testHasPermissionDenied() {
        Authentication auth = createAuthentication(Set.of("USER_MANAGE"));
        assertFalse(permissionEvaluator.hasPermission(auth, null, "PASSWORD_ADMIN_RESET"));
    }

    @Test
    void testNullAuthentication() {
        assertFalse(permissionEvaluator.hasPermission(null, null, "USER_MANAGE"));
        assertFalse(permissionEvaluator.hasPermission(null, 1L, "User", "USER_MANAGE"));
    }
}
