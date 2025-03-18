package com.toiter.userservice.model;

import com.toiter.userservice.entity.Message;

public class MessageSentEvent {
    private Message message;

    public MessageSentEvent() {
    }

    public MessageSentEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

}
