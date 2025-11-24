package com.toiter.userservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class ChatData {
    private Long chatId;
    private String receiverUsername;
    private String lastMessageSender;
    private String lastMessageContent;
    private LocalDateTime lastMessageSentDate;
    @JsonIgnore
    private Long receiverId;
    private String receiverProfileImageUrl;

    public ChatData() {
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Long receiverId) {
        this.chatId = chatId;
        this.receiverUsername = receiverUsername;
        this.lastMessageSender = lastMessageSender;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentDate = lastMessageSentDate;
        this.receiverId = receiverId;
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Number receiverId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate, receiverId != null ? receiverId.longValue() : null);
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Integer receiverId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate, receiverId != null ? receiverId.longValue() : null);
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, int receiverId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate, Long.valueOf(receiverId));
    }

    public LocalDateTime getLastMessageSentDate() {
        return lastMessageSentDate;
    }

    public void setLastMessageSentDate(LocalDateTime lastMessageSentDate) {
        this.lastMessageSentDate = lastMessageSentDate;
    }

    public String getLastMessageContent() {
        return lastMessageContent;
    }

    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverProfileImageUrl() {
        return receiverProfileImageUrl;
    }

    public void setReceiverProfileImageUrl(String receiverProfileImageUrl) {
        this.receiverProfileImageUrl = receiverProfileImageUrl;
    }
}