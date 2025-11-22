package com.toiter.userservice.config;

import com.toiter.userservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Value("${service.shared-key}")
    private String sharedKey;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.info("Request: " + request.getMethod() + " " + request.getRequestURI());

        String path = request.getRequestURI();

        logger.debug(String.format("Path: %s", path));

        if (path.startsWith("/auth/") || path.startsWith("/api/auth/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/images")) {
            logger.debug("Ignorando validação JWT para rota pública");
            filterChain.doFilter(request, response);
            return;
        }

        // Validação para /internal/** com token compartilhado
        if (path.startsWith("/internal/") || path.startsWith("/api/internal/")) {
            logger.debug("Validando token compartilhado para rota /internal/**");
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.equals("Bearer " + sharedKey)) {
                logger.warn("Acesso não autorizado para /internal/**");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Acesso não autorizado para /internal/**");
                return;
            }

            logger.debug("Token compartilhado válido para rota /internal/**");
            filterChain.doFilter(request, response);
            return;
        }

        // Extrair JWT (de cookie ou header)
        String jwt = extractJwt(request);

        // Se não houver JWT, apenas continuar sem autenticar
        if (jwt == null) {
            logger.debug("Token JWT não encontrado");
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("Token JWT encontrado");

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isTokenValid(jwt)) {
                    Long userId = jwtService.extractUserId(jwt);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug(String.format("Usuário %d autenticado com sucesso", userId));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("Erro ao validar token JWT: %s", e.getMessage()));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token JWT inválido ou malformado");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o JWT da requisição, primeiro tentando do cookie HttpOnly,
     * depois do header Authorization como fallback.
     */
    private String extractJwt(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Para rotas não-/internal/**, tentar cookie primeiro
        if (!path.startsWith("/internal/") && !path.startsWith("/api/internal/")) {
            // Tentar extrair do cookie accessToken
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        String token = cookie.getValue();
                        if (token != null && !token.isEmpty()) {
                            logger.debug("JWT extraído do cookie accessToken");
                            return token;
                        }
                    }
                }
            }
        }

        // Fallback: tentar header Authorization
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            logger.debug("JWT extraído do header Authorization");
            return authHeader.substring(7);
        }

        return null;
    }
}
