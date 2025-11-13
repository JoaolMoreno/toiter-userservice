package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class ChatData {
    private Long chatId;
    private String receiverUsername;
    private String lastMessageSender;
    private String lastMessageContent;
    private LocalDateTime lastMessageSentDate;

    public ChatData() {
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate) {
        this.chatId = chatId;
        this.receiverUsername = receiverUsername;
        this.lastMessageSender = lastMessageSender;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentDate = lastMessageSentDate;
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
}