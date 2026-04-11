package com.scholarops.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Maps access-denied to 401 when there is no fully authenticated principal, and 403 when a
 * real user lacks permission. Uses {@link AuthenticationTrustResolver} so behavior matches
 * Spring Security (e.g. {@code null} security context is "not authenticated", not anonymous).
 */
@Component
public class AnonymousAwareAccessDeniedHandler implements AccessDeniedHandler {

    private static final String JSON_UNAUTHORIZED =
            "{\"success\":false,\"error\":\"Authentication required\"}";
    private static final String JSON_FORBIDDEN =
            "{\"success\":false,\"error\":\"Access denied\"}";

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean insufficientAuth = !trustResolver.isAuthenticated(auth);
        if (insufficientAuth) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
                ? JSON_UNAUTHORIZED : JSON_FORBIDDEN);
    }
}
