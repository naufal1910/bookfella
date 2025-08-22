package com.bookfella.booking.service;

import com.bookfella.booking.domain.Reservation;
import com.bookfella.booking.dto.CreateReservationRequest;
import com.bookfella.booking.dto.ReservationResponse;
import com.bookfella.booking.messaging.ReservationCreatedEvent;
import com.bookfella.booking.repo.ReservationRepository;
import com.bookfella.booking.util.KeyUtils;
import com.bookfella.booking.web.ConflictException;
import com.bookfella.booking.web.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository repository;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher publisher;

    public ReservationService(ReservationRepository repository, StringRedisTemplate redis, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.redis = redis;
        this.publisher = publisher;
    }

    @Transactional
    public ReservationResponse create(String idempotencyKey, CreateReservationRequest req) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        String redisKey = "idem:" + idempotencyKey;
        boolean firstTime = Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(KeyUtils.IDEMPOTENCY_TTL_SECONDS))
        );
        if (!firstTime) {
            throw new ConflictException("Duplicate request");
        }

        Reservation r = new Reservation();
        r.setId(UUID.randomUUID().toString());
        r.setUserId(req.userId());
        r.setHotelId(req.hotelId());
        r.setCheckIn(LocalDate.parse(req.checkIn()));
        r.setCheckOut(LocalDate.parse(req.checkOut()));
        r.setTotalPrice(req.totalPrice());
        r.setStatus("CREATED");

        r = repository.save(r);

        // Publish domain event; actual Kafka publish happens in @TransactionalEventListener AFTER_COMMIT
        publisher.publishEvent(new ReservationCreatedEvent(
                r.getId(), r.getUserId(), r.getHotelId(), r.getCheckIn(), r.getCheckOut(), r.getTotalPrice()
        ));
        return toDto(r);
    }

    public ReservationResponse getById(String id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getUserId(),
                r.getHotelId(),
                r.getCheckIn().toString(),
                r.getCheckOut().toString(),
                r.getTotalPrice(),
                r.getStatus(),
                Optional.ofNullable(r.getCreatedAt()).map(Object::toString).orElse(null)
        );
    }
}
