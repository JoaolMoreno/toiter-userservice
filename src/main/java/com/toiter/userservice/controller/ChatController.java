package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.entity.Message;
import com.toiter.userservice.model.ChatData;
import com.toiter.userservice.service.ChatService;
import com.toiter.userservice.service.UserService;
import com.toiter.userservice.service.JwtService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final JwtService jwtService;

    public ChatController(ChatService chatService, UserService userService, JwtService jwtService) {
        this.chatService = chatService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /** Criar um novo chat entre dois usuários */
    @PostMapping("/start/{username}")
    public ResponseEntity<Chat> startChat(
            @PathVariable @NotNull String username,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        Long userId = jwtService.extractUserId(token);
        Long otherUserId = userService.getUserByUsername(username).getId();

        if (userId.equals(otherUserId)) {
            throw new RuntimeException("Você não pode iniciar um chat consigo mesmo");
        }

        Chat chat = chatService.createChat(userId, otherUserId);
        return new ResponseEntity<>(chat, HttpStatus.CREATED);
    }

    /** Enviar uma mensagem em um chat */
    @PostMapping("/{chatId}/message")
    public ResponseEntity<Message> sendMessage(
            @PathVariable Long chatId,
            @RequestBody @NotNull String content,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        Long senderId = jwtService.extractUserId(token);

        Message message = chatService.sendMessage(chatId, senderId, content);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    /** Recuperar mensagens de um chat, paginadas */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<Message>> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        Long userId = jwtService.extractUserId(token);

        // Verificar se o usuário pertence ao chat
        chatService.getChatById(chatId, userId);

        Page<Message> messages = chatService.getMessages(chatId, page, size);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    /** Recuperar a lista de chats do usuário autenticado */
    @GetMapping("/my-chats")
    public ResponseEntity<Page<ChatData>> getMyChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") @Parameter(description = "Token de autenticação Bearer") String token) {
        Long userId = jwtService.extractUserId(token);

        Page<ChatData> chats = chatService.getChatsForUser(userId, page, size);
        return new ResponseEntity<>(chats, HttpStatus.OK);
    }
}