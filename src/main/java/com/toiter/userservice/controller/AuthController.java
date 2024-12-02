package com.toiter.userservice.controller;

import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.model.UserRequest;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Operation(summary = "Registrar um novo usuário", description = "Registra um novo usuário e retorna uma mensagem de sucesso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do usuário inválidos")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRequest userRequest) {
        userService.registerUser(userRequest);
        return ResponseEntity.ok("Usuário registrado com sucesso");
    }

    @Operation(summary = "Login de um usuário", description = "Autentica o usuário e retorna um token de acesso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.authenticateAndGenerateTokens(loginRequest);
        return ResponseEntity.ok(tokenResponse);
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
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid @NotNull TokenResponse tokenResponse) {
        TokenResponse newTokenResponse = authService.refreshTokens(tokenResponse);
        return ResponseEntity.ok(newTokenResponse);
    }
}