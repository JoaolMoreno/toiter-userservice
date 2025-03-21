package com.toiter.userservice.model;

import java.time.LocalDateTime;

public class MessageData {
    private Long id;
    private Long chatId;
    private String message;
    private LocalDateTime sentDate;

    public MessageData() {}

    public MessageData(Long id, Long chatId, String message, LocalDateTime sentDate) {
        this.id = id;
        this.chatId = chatId;
        this.message = message;
        this.sentDate = sentDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {}
}
