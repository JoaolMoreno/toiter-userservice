package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.entity.Message;
import com.toiter.userservice.model.ChatData;
import com.toiter.userservice.service.AuthService;
import com.toiter.userservice.service.ChatService;
import com.toiter.userservice.service.UserService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final AuthService authService;

    @Autowired
    public ChatController(ChatService chatService, UserService userService, AuthService authService) {
        this.chatService = chatService;
        this.userService = userService;
        this.authService = authService;
    }

    /** Criar um novo chat entre dois usuários */
    @PostMapping("/start/{username}")
    public ResponseEntity<Chat> startChat(
            @PathVariable @NotNull String username,
            Authentication authentication) {
        Long userId = authService.getUserIdFromAuthentication(authentication);
        Long otherUserId = userService.getUserByUsername(username).getId();

        if (userId.equals(otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voce nao pode iniciar um chat com voce mesmo");
        }

        Chat chat = chatService.createChat(userId, otherUserId);
        return new ResponseEntity<>(chat, HttpStatus.CREATED);
    }

    /** Enviar uma mensagem em um chat */
    @PostMapping("/{chatId}/message")
    public ResponseEntity<Message> sendMessage(
            @PathVariable Long chatId,
            @NotNull @Size(min = 1, max = 10000) String content,
            Authentication authentication) {
        Long senderId = authService.getUserIdFromAuthentication(authentication);

        Message message = chatService.sendMessage(chatId, senderId, content);
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    @MessageMapping("/chat/{chatId}/message")
    public void sendMessageWs(
            @DestinationVariable Long chatId,
            @NotNull @Size(min = 1, max = 10000) String content,
            Authentication authentication) {
        Long senderId = authService.getUserIdFromAuthentication(authentication);

        chatService.sendMessage(chatId, senderId, content);
    }

    /** Recuperar mensagens de um chat, paginadas */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<Message>> getMessages(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = authService.getUserIdFromAuthentication(authentication);

        // Verificar se o usuário pertence ao chat
        try{
            chatService.getChatById(chatId, userId);
        }
        catch (NoSuchElementException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

        Page<Message> messages = chatService.getMessages(chatId, page, size);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    /** Recuperar a lista de chats do usuário autenticado */
    @GetMapping("/my-chats")
    public ResponseEntity<Page<ChatData>> getMyChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = authService.getUserIdFromAuthentication(authentication);

        Page<ChatData> chats = chatService.getChatsForUser(userId, page, size);
        return new ResponseEntity<>(chats, HttpStatus.OK);
    }
}