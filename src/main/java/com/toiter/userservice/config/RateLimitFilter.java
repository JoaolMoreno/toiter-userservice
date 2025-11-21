package com.toiter.userservice.config;

import com.toiter.userservice.service.JwtService;
import com.toiter.userservice.service.RateLimitService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to apply rate limiting to requests based on user ID.
 * This filter runs before authentication to check rate limits.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final JwtService jwtService;

    public RateLimitFilter(RateLimitService rateLimitService, JwtService jwtService) {
        this.rateLimitService = rateLimitService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for certain paths
        if (shouldSkipRateLimiting(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user ID from JWT token
        Long userId = extractUserIdFromRequest(request);
        
        // Extract IP address from request
        String ipAddress = extractIpAddress(request);

        // Determine request type
        RateLimitService.RequestType requestType = determineRequestType(path, method);

        // Check rate limit
        if (!rateLimitService.isAllowed(userId, ipAddress, requestType)) {
            // Rate limit exceeded
            long resetTime = rateLimitService.getResetTime(userId, ipAddress, requestType);
            int limit = rateLimitService.getLimitForType(requestType);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + resetTime));
            response.setHeader("Retry-After", String.valueOf(resetTime));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again in %d seconds.\"}",
                resetTime
            ));
            return;
        }

        // Add rate limit headers to response
        long remaining = rateLimitService.getRemainingRequests(userId, ipAddress, requestType);
        long resetTime = rateLimitService.getResetTime(userId, ipAddress, requestType);
        int limit = rateLimitService.getLimitForType(requestType);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + resetTime));

        filterChain.doFilter(request, response);
    }

    /**
     * Determine if rate limiting should be skipped for this path.
     */
    private boolean shouldSkipRateLimiting(String path) {
        // Skip rate limiting for static resources and documentation
        return path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/images/") ||
               path.startsWith("/internal/") ||
               path.startsWith("/api/internal/") ||
               path.equals("/auth/logout") ||
               path.equals("/auth/check-session");
    }

    /**
     * Extract user ID from JWT token in the request.
     */
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        String jwt = extractJwt(request);
        
        if (jwt == null) {
            return null;
        }

        try {
            return jwtService.extractUserId(jwt);
        } catch (JwtException e) {
            // If token is invalid or expired, treat as unauthenticated
            return null;
        }
    }

    /**
     * Extract IP address from the request, considering proxy headers.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // If X-Forwarded-For contains multiple IPs, take the first one (original client IP)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    /**
     * Extract JWT from cookie or Authorization header.
     */
    private String extractJwt(HttpServletRequest request) {
        // Try cookie first
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback to Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    /**
     * Determine the request type for rate limiting purposes.
     */
    private RateLimitService.RequestType determineRequestType(String path, String method) {
        // Login endpoint has the most restrictive rate limit
        if (path.equals("/auth/login") || path.equals("/api/auth/login")) {
            return RateLimitService.RequestType.LOGIN;
        }

        // GET requests have higher limits
        if (HttpMethod.GET.matches(method)) {
            return RateLimitService.RequestType.GET;
        }

        // All other methods (POST, PUT, DELETE, etc.) have more restrictive limits
        return RateLimitService.RequestType.OTHER;
    }
}
