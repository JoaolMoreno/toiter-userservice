package com.toiter.userservice.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendFollowCreatedEvent(Object event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("follow-created-topic", event);
            return true;
        });
    }

    public void sendFollowDeletedEvent(Object event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("follow-deleted-topic", event);
            return true;
        });
    }

    public void sendUserUpdatedEvent(Object event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("user-updated-topic", event);
            return true;
        });
    }
}
