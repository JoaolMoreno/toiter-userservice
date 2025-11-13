package com.toiter.userservice.model;

import com.toiter.userservice.entity.Chat;

public class ChatCreatedEvent {
    private Chat chat;

    public ChatCreatedEvent() {
    }

    public ChatCreatedEvent(Chat chat) {
        this.chat = chat;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
}
