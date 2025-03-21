package com.toiter.userservice.service;

import com.toiter.userservice.entity.Chat;
import com.toiter.userservice.model.ChatCreatedEvent;
import com.toiter.userservice.model.ChatData;
import com.toiter.userservice.entity.Message;
import com.toiter.userservice.model.MessageData;
import com.toiter.userservice.model.MessageSentEvent;
import com.toiter.userservice.producer.KafkaProducer;
import com.toiter.userservice.repository.ChatRepository;
import com.toiter.userservice.repository.MessageRepository;
import com.toiter.userservice.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KafkaProducer kafkaProducer;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, UserRepository userRepository, KafkaProducer kafkaProducer) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public Chat createChat(Long user1Id, Long user2Id) {
        // Garantir user1Id < user2Id para evitar duplicatas
        Long smallerId = Math.min(user1Id, user2Id);
        Long largerId = Math.max(user1Id, user2Id);

        Optional<Chat> existingChat = chatRepository.findByUserIds(smallerId, largerId);
        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        Chat chat = new Chat(
                smallerId,
                largerId
        );
        chatRepository.save(chat);

        kafkaProducer.sendChatCreatedEvent(new ChatCreatedEvent(chat));

        return chat;
    }

    public MessageData sendMessage(Long chatId, Long senderId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        if (!chat.getUserId1().equals(senderId) && !chat.getUserId2().equals(senderId)) {
            throw new NoSuchElementException("User is not part of this chat");
        }

        if (!userRepository.existsById(senderId)) {
            throw new NoSuchElementException("Sender not found");
        }

        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setSentDate(LocalDateTime.now());
        messageRepository.save(message);

        kafkaProducer.sendMessageSentEvent(new MessageSentEvent(message));

        return convertToMessageData(message);
    }

    public Page<MessageData> getMessages(Long chatId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentDate").descending());
        Page<Message> messages = messageRepository.findByChatIdOrderBySentDateDesc(chatId, pageable);

        List<MessageData> messageDataList = messages.getContent().stream()
                .map(this::convertToMessageData)
                .collect(Collectors.toList());

        return new PageImpl<>(messageDataList, pageable, messages.getTotalElements());
    }

    public Page<ChatData> getChatsForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatRepository.findChatDataByUserId(userId, pageable);
    }

    private MessageData convertToMessageData(Message message) {
        return new MessageData(
                message.getId(),
                message.getChat().getId(),
                message.getContent(),
                message.getSentDate()
        );
    }

    public Chat getChatById(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        if (!chat.getUserId1().equals(userId) && !chat.getUserId2().equals(userId)) {
            throw new IllegalArgumentException("User is not part of this chat");
        }

        return chat;
    }
}