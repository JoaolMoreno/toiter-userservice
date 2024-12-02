package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Follow;
import com.toiter.userservice.service.FollowService;
import com.toiter.userservice.service.JwtService;
import com.toiter.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/follows")
@Tag(name = "Follow Controller", description = "APIs relacionadas as Interações de Follow/Unfollow entre usuarios")
public class FollowController {

    private final FollowService followService;
    private final UserService userService;
    private final JwtService jwtService;

    public FollowController(FollowService followService, UserService userService, JwtService jwtService) {
        this.followService = followService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @GetMapping("/{username}/followers")
    @Operation(
            summary = "Obter seguidores de um usuário",
            description = "Retorna a lista de seguidores de um usuário específico",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de seguidores retornada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Follow.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            }
    )
    public List<Follow> getFollowers(@PathVariable @NotNull String username) {
        Long userId = userService.getUserByUsername(username).getId();
        return followService.getFollowers(userId);
    }

    @GetMapping("/{username}/followings")
    @Operation(
            summary = "Obter seguidos de um usuário",
            description = "Retorna a lista de usuários que um usuário segue",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de seguidos retornada com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Follow.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            }
    )
    public List<Follow> getFollowings(@PathVariable @NotNull String username) {
        Long userId = userService.getUserByUsername(username).getId();
        return followService.getFollowings(userId);
    }

    @PostMapping("/{username}/follow")
    @Operation(
            summary = "Seguir um usuário",
            description = "Permite que um usuário siga outro. O usuário não pode seguir a si mesmo",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Usuário seguido com sucesso",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Follow.class))),
                    @ApiResponse(responseCode = "400", description = "Não é possível seguir a si mesmo"),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            }
    )
    public Follow followUser(
            @PathVariable @NotNull String username,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        String followerUsername = extractUsernameFromToken(token);
        Long followerId = userService.getUserByUsername(followerUsername).getId();
        Long userId = userService.getUserByUsername(username).getId();
        if(followerId.equals(userId)) {
            throw new RuntimeException("Você não pode seguir a si mesmo");
        }
        return followService.followUser(userId, followerId);
    }

    @DeleteMapping("/{username}/unfollow")
    @Operation(
            summary = "Deixar de seguir um usuário",
            description = "Permite que um usuário deixe de seguir outro. O usuário não pode deixar de seguir a si mesmo",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Usuário deixado de seguir com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Não é possível deixar de seguir a si mesmo"),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            }
    )
    public void unfollowUser(
            @PathVariable @NotNull String username,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        String followerUsername = extractUsernameFromToken(token);
        Long followerId = userService.getUserByUsername(followerUsername).getId();
        Long userId = userService.getUserByUsername(username).getId();
        if(followerId.equals(userId)) {
            throw new RuntimeException("Você não pode deixar de seguir a si mesmo");
        }
        followService.unfollowUser(userId, followerId);
    }

    private String extractUsernameFromToken(@NotNull String token) {
        String jwt = token.replace("Bearer ", "");
        return jwtService.extractUsername(jwt);
    }
}
