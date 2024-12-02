package com.toiter.userservice.controller;

import com.toiter.userservice.model.UpdatedUser;
import com.toiter.userservice.model.UserPublicData;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "User Controller", description = "APIs relacionadas aos usuários")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PutMapping("/")
    @Operation(
            summary = "Atualizar dados do usuário",
            description = "Atualiza os dados do usuário autenticado com base no objeto fornecido",
            security = {@SecurityRequirement(name = "bearerAuth")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Objeto com os dados atualizados do usuário",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdatedUser.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Dados atualizados com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos"),
                    @ApiResponse(responseCode = "401", description = "Não autorizado")
            }
    )
    public ResponseEntity<Void> updateUser(
            @RequestBody @NotNull UpdatedUser updatedUser,
            Authentication authentication) {
        Long id = authService.getUserIdFromAuthentication(authentication);
        userService.updateUser(id, updatedUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile-image")
    @Operation(
            summary = "Atualizar imagem de perfil",
            description = "Atualiza a imagem de perfil do usuário autenticado",
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Imagem de perfil atualizada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Formato de imagem inválido"),
                    @ApiResponse(responseCode = "401", description = "Não autorizado")
            }
    )
    public ResponseEntity<Void> updateProfileImage(
            @RequestParam("image")
            @Parameter(
                    description = "Arquivo de imagem para o perfil",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            ) MultipartFile image,
            Authentication authentication) throws IOException {
        Long userId = authService.getUserIdFromAuthentication(authentication);
        userService.updateProfileImage(userId, image);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/header-image")
    @Operation(
            summary = "Atualizar imagem de cabeçalho",
            description = "Atualiza a imagem de cabeçalho do usuário autenticado",
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Imagem de cabeçalho atualizada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Formato de imagem inválido"),
                    @ApiResponse(responseCode = "401", description = "Não autorizado")
            }
    )
    public ResponseEntity<Void> updateHeaderImage(
            @RequestParam("image")
            @Parameter(
                    description = "Arquivo de imagem para o cabeçalho",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data")
            ) MultipartFile image,
            Authentication authentication) throws IOException {
        Long userId = authService.getUserIdFromAuthentication(authentication);
        userService.updateHeaderImage(userId, image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Obter dados públicos do usuário",
            description = "Retorna os dados públicos do usuário com base no nome de usuário fornecido",
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados do usuário retornados com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserPublicData.class)
                            )),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            }
    )
    public UserPublicData getPublicUserData(
            @PathVariable @NotNull @Parameter(description = "Nome de usuário") String username,
            Authentication authentication) {
        Long authenticatedUserId = authService.getUserIdFromAuthentication(authentication);
        return userService.getPublicUserDataByUsername(username, authenticatedUserId);
    }

    @GetMapping("/query")
    @Operation(
            summary = "Buscar usuários por nome de usuário",
            description = "Retorna uma lista paginada de nomes de usuários que correspondem ao filtro fornecido",
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Parâmetros de busca inválidos")
            }
    )
    public Map<String, Object> getExistingUsers(
            @RequestParam @Parameter(description = "Parte do nome de usuário para busca") String username,
            @RequestParam @Parameter(description = "Número da página") int page,
            @RequestParam @Parameter(description = "Tamanho da página") int size) {
        Page<String> users = userService.getExistingUsers(username, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", users.getContent());
        response.put("page", users.getNumber());
        response.put("size", users.getSize());
        response.put("totalElements", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());

        return response;
    }
}
