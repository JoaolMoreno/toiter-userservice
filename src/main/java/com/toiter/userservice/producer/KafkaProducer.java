package com.toiter.userservice.producer;

import com.toiter.userservice.model.FollowCreatedEvent;
import com.toiter.userservice.model.FollowDeletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendFollowCreatedEvent(FollowCreatedEvent event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("follow-events-topic", event);
            return true;
        });
    }

    public void sendFollowDeletedEvent(FollowDeletedEvent event) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("follow-events-topic", event);
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
