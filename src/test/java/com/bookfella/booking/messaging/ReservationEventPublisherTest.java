package com.bookfella.booking.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ReservationEventPublisherTest {

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    ReservationEventPublisher eventPublisher;

    @Test
    void publishesToTopic() {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                "r1", "u1", "h1", LocalDate.parse("2025-09-01"), LocalDate.parse("2025-09-03"), new BigDecimal("199.99")
        );
        eventPublisher.onReservationCreated(event);
        verify(kafkaTemplate).send(argThat((ProducerRecord<String, Object> rec) -> {
            assertThat(rec.topic()).isEqualTo(ReservationEventPublisher.TOPIC);
            assertThat(rec.key()).isEqualTo("r1");
            return true;
        }));
    }
}
