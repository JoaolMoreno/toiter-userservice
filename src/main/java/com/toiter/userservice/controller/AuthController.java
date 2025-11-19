package com.toiter.userservice.controller;

import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.LoginResponse;
import com.toiter.userservice.model.SessionResponse;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.model.UserRequest;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.JwtService;
import com.toiter.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    @Value("${JWT_ACCESS_TOKEN_EXPIRATION}")
    private Integer JWT_ACCESS_TOKEN_EXPIRATION;

    @Value("${JWT_REFRESH_TOKEN_EXPIRATION}")
    private Integer JWT_REFRESH_TOKEN_EXPIRATION;

    public AuthController(AuthService authService, UserService userService, JwtService jwtService) {
        this.authService = authService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Registrar um novo usuário", description = "Registra um novo usuário e retorna uma mensagem de sucesso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do usuário inválidos")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRequest userRequest) {
        userService.registerUser(userRequest);
        return ResponseEntity.ok("Usuário registrado com sucesso");
    }

    @Operation(summary = "Login de um usuário", description = "Autentica o usuário e retorna um token de acesso via cookie HttpOnly")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.authenticateAndGenerateTokens(loginRequest);

        Cookie refreshCookie = new Cookie("refresh_token", tokenResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge(JWT_REFRESH_TOKEN_EXPIRATION);
        response.addCookie(refreshCookie);

        Cookie accessCookie = new Cookie("accessToken", tokenResponse.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(JWT_ACCESS_TOKEN_EXPIRATION);
        response.addCookie(accessCookie);

        // Não retornar o token no corpo da resposta por segurança
        return ResponseEntity.ok(new LoginResponse(tokenResponse.getExpiresIn(), "Login realizado com sucesso"));
    }

    @Operation(summary = "Atualizar token de acesso",
            description = "Atualiza um token de acesso expirado",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token de atualização inválido")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        TokenResponse newTokenResponse = authService.refreshTokens(request);
        return ResponseEntity.ok(newTokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        return ResponseEntity.ok("Logout bem-sucedido");
    }

    @Operation(summary = "Verificar a validade da sessão do usuário",
            description = "Valida o token de acesso do cookie HttpOnly e retorna informações sobre a sessão")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão válida"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
    })
    @GetMapping("/check-session")
    public ResponseEntity<SessionResponse> checkSession(HttpServletRequest request) {
        // Extrair token do cookie
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Fallback para header Authorization (para compatibilidade com clientes não-browser)
        if (token == null) {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            }
        }

        if (token == null) {
            return ResponseEntity.status(401).body(new SessionResponse(false, 0));
        }

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(new SessionResponse(false, 0));
        }

        long expiresIn = jwtService.getTokenExpiration(token).getTime() / 1000;

        return ResponseEntity.ok(new SessionResponse(true, expiresIn));
    }

}