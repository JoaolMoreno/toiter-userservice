package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class ChatData {
    private Long chatId;
    private String receiverUsername;
    private String lastMessageSender;
    private String lastMessageContent;
    private LocalDateTime lastMessageSentDate;
    private String receiverProfileImageUrl;
    private Long receiverProfileImageId;

    public ChatData() {
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate) {
        this.chatId = chatId;
        this.receiverUsername = receiverUsername;
        this.lastMessageSender = lastMessageSender;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentDate = lastMessageSentDate;
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Long receiverProfileImageId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate);
        this.receiverProfileImageId = receiverProfileImageId;
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Number receiverProfileImageId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate);
        this.receiverProfileImageId = receiverProfileImageId != null ? receiverProfileImageId.longValue() : null;
    }

    // Overloads to match potential primitive/wrapper int cases from JPQL CASE
    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, Integer receiverProfileImageId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate);
        this.receiverProfileImageId = receiverProfileImageId != null ? receiverProfileImageId.longValue() : null;
    }

    public ChatData(Long chatId, String receiverUsername, String lastMessageSender, String lastMessageContent, LocalDateTime lastMessageSentDate, int receiverProfileImageId) {
        this(chatId, receiverUsername, lastMessageSender, lastMessageContent, lastMessageSentDate);
        this.receiverProfileImageId = Long.valueOf(receiverProfileImageId);
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

    public String getReceiverProfileImageUrl() {
        return receiverProfileImageUrl;
    }

    public void setReceiverProfileImageUrl(String receiverProfileImageUrl) {
        this.receiverProfileImageUrl = receiverProfileImageUrl;
    }

    public Long getReceiverProfileImageId() {
        return receiverProfileImageId;
    }

    public void setReceiverProfileImageId(Long receiverProfileImageId) {
        this.receiverProfileImageId = receiverProfileImageId;
    }
}