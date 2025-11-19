package com.toiter.userservice.config;

import com.toiter.userservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtService);
        ReflectionTestUtils.setField(filter, "sharedKey", "test-shared-key");
        SecurityContextHolder.clearContext();
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void testPublicRouteAuth_SkipsAuthentication() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testPublicRouteSwagger_SkipsAuthentication() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testInternalRouteWithValidSharedKey_Succeeds() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer test-shared-key");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testInternalRouteWithInvalidSharedKey_Returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-key");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).getWriter();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testInternalRouteWithoutAuthHeader_Returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/internal/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testAuthenticationWithValidCookie_Succeeds() throws Exception {
        String validToken = "valid-jwt-token";
        Cookie accessTokenCookie = new Cookie("accessToken", validToken);
        
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractUsername(validToken)).thenReturn("testuser");
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(123L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(123L, auth.getPrincipal());
    }

    @Test
    void testAuthenticationWithValidHeader_Succeeds() throws Exception {
        String validToken = "valid-jwt-token";
        
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtService.extractUsername(validToken)).thenReturn("testuser");
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(123L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(123L, auth.getPrincipal());
    }

    @Test
    void testAuthenticationCookieTakesPriorityOverHeader() throws Exception {
        String cookieToken = "cookie-jwt-token";
        String headerToken = "header-jwt-token";
        Cookie accessTokenCookie = new Cookie("accessToken", cookieToken);
        
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);
        when(jwtService.extractUsername(cookieToken)).thenReturn("testuser");
        when(jwtService.isTokenValid(cookieToken)).thenReturn(true);
        when(jwtService.extractUserId(cookieToken)).thenReturn(123L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Should use cookie token, not header token
        verify(jwtService).extractUsername(cookieToken);
        verify(jwtService, never()).extractUsername(headerToken);
    }

    @Test
    void testAuthenticationWithInvalidToken_Returns401() throws Exception {
        String invalidToken = "invalid-jwt-token";
        Cookie accessTokenCookie = new Cookie("accessToken", invalidToken);
        
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractUsername(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testAuthenticationWithExpiredToken_Returns401() throws Exception {
        String expiredToken = "expired-jwt-token";
        Cookie accessTokenCookie = new Cookie("accessToken", expiredToken);
        
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractUsername(expiredToken)).thenReturn("testuser");
        when(jwtService.isTokenValid(expiredToken)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
    }

    @Test
    void testAuthenticationWithNoToken_ContinuesWithoutAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/users/me");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
    }

    @Test
    void testWebSocketHandshakeWithCookie_Authenticates() throws Exception {
        String validToken = "valid-jwt-token";
        Cookie accessTokenCookie = new Cookie("accessToken", validToken);
        
        when(request.getRequestURI()).thenReturn("/chat");
        when(request.getMethod()).thenReturn("GET");
        when(request.getCookies()).thenReturn(new Cookie[]{accessTokenCookie});
        when(jwtService.extractUsername(validToken)).thenReturn("testuser");
        when(jwtService.isTokenValid(validToken)).thenReturn(true);
        when(jwtService.extractUserId(validToken)).thenReturn(123L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(123L, auth.getPrincipal());
    }
}
