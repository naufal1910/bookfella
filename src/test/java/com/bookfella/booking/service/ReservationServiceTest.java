package com.bookfella.booking.service;

import com.bookfella.booking.domain.Reservation;
import com.bookfella.booking.dto.CreateReservationRequest;
import com.bookfella.booking.dto.ReservationResponse;
import com.bookfella.booking.repo.ReservationRepository;
import com.bookfella.booking.messaging.ReservationCreatedEvent;
import com.bookfella.booking.util.KeyUtils;
import com.bookfella.booking.web.ConflictException;
import com.bookfella.booking.web.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    ReservationRepository repository;

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> valueOps;

    ReservationService service;
    @Mock
    ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        service = new ReservationService(repository, redis, publisher);
    }

    @Test
    void create_firstTime_setsIdemKeyWithTTL600_andSaves() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("idem:k1"), eq("1"), any(Duration.class))).thenReturn(true);
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReservationRequest req = new CreateReservationRequest(
                "u1", "h1", "2025-09-01", "2025-09-03", new BigDecimal("199.99")
        );
        ReservationResponse out = service.create("k1", req);
        assertThat(out.id()).isNotBlank();

        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).setIfAbsent(eq("idem:k1"), eq("1"), ttlCap.capture());
        assertThat(ttlCap.getValue()).isEqualTo(Duration.ofSeconds(KeyUtils.IDEMPOTENCY_TTL_SECONDS));
        verify(publisher).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void create_duplicate_throwsConflict() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("idem:kdup"), eq("1"), any(Duration.class))).thenReturn(false);

        CreateReservationRequest req = new CreateReservationRequest(
                "u1", "h1", "2025-09-01", "2025-09-03", new BigDecimal("199.99")
        );
        assertThrows(ConflictException.class, () -> service.create("kdup", req));
        verify(repository, never()).save(any());
    }

    @Test
    void getById_notFound_throws() {
        when(repository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getById("nope"));
    }
}
