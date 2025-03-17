package com.toiter.userservice.service;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.model.ChatData;
import com.toiter.userservice.entity.Message;
import com.toiter.userservice.entity.User;
import com.toiter.userservice.repository.ChatRepository;
import com.toiter.userservice.repository.MessageRepository;
import com.toiter.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /** Criar um novo chat entre dois usuários */
    public Chat createChat(Long user1Id, Long user2Id) {
        // Garantir user1Id < user2Id para evitar duplicatas
        Long smallerId = Math.min(user1Id, user2Id);
        Long largerId = Math.max(user1Id, user2Id);

        // Verificar se já existe um chat entre esses usuários
        Optional<Chat> existingChat = chatRepository.findByUserIds(smallerId, largerId);
        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        // Criar novo chat
        Chat chat = new Chat(
                smallerId,
                largerId
        );
        chatRepository.save(chat);

        // Chamada comentada para o Kafka Producer
        // kafkaProducer.sendMessage("chat_created", chat.getId());

        return chat;
    }

    /** Enviar uma mensagem em um chat */
    public Message sendMessage(Long chatId, Long senderId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        // Verificar se o usuário faz parte do chat
        if (!chat.getUserId1().equals(senderId) && !chat.getUserId2().equals(senderId)) {
            throw new IllegalArgumentException("User is not part of this chat");
        }

        // Verificar se o remetente existe
        if (!userRepository.existsById(senderId)) {
            throw new IllegalArgumentException("Sender not found");
        }

        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentDate(LocalDateTime.now());
        messageRepository.save(message);

        // Chamada comentada para o Kafka Producer
        // kafkaProducer.sendMessage("message_sent", message.getId());

        return message;
    }

    /** Recuperar mensagens de um chat, paginadas e ordenadas por data descendente */
    public Page<Message> getMessages(Long chatId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentDate").descending());
        return messageRepository.findByChatIdOrderBySentDateDesc(chatId, pageable);
    }

    /** Recuperar a lista de chats de um usuário com a última mensagem */
    public Page<ChatData> getChatsForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatRepository.findChatDataByUserId(userId, pageable);
    }

    /** Buscar um chat pelo ID e verificar se o usuário pertence a ele */
    public Chat getChatById(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        if (!chat.getUserId1().equals(userId) && !chat.getUserId2().equals(userId)) {
            throw new IllegalArgumentException("User is not part of this chat");
        }

        return chat;
    }
}