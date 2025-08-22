package com.bookfella.booking.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBean(KafkaTemplate.class)
public class ReservationEventPublisher {

    public static final String TOPIC = "reservations.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReservationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, event.id(), event);
        kafkaTemplate.send(record);
    }
}
