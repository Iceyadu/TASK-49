package com.scholarops.controller.support;

import com.scholarops.config.SecurityConfig;
import com.scholarops.security.AnonymousAwareAccessDeniedHandler;
import com.scholarops.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;

/**
 * Loads application HTTP security + method security for {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}
 * slices. JWT filter is mocked and passes requests through; stub {@link PermissionEvaluator} in tests that need it.
 */
@Import({SecurityConfig.class, AnonymousAwareAccessDeniedHandler.class})
public abstract class AbstractWebMvcControllerTest {

    @MockBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    protected PermissionEvaluator permissionEvaluator;

    @BeforeEach
    void jwtFilterPassesThrough() throws Exception {
        org.mockito.Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    protected void grantAllEvaluatorPermissions() {
        lenient().when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                .thenReturn(true);
        lenient().when(permissionEvaluator.hasPermission(
                any(Authentication.class), nullable(Serializable.class), anyString(), any()))
                .thenReturn(true);
    }
}
