package com.scholarops.controller.support;

import com.scholarops.model.entity.User;
import com.scholarops.security.UserDetailsImpl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class WebMvcTestUsers {

    private WebMvcTestUsers() {
    }

    public static UserDetailsImpl userDetails(Long id, String username, String role, String... permissions) {
        User u = User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.local")
                .passwordHash("{noop}x")
                .fullName(username)
                .enabled(true)
                .accountLocked(false)
                .build();
        Set<String> permSet = Arrays.stream(permissions).collect(Collectors.toSet());
        return new UserDetailsImpl(u, Set.of(role), permSet);
    }
}
