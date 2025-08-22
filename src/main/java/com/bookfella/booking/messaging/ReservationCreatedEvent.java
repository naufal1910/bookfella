package com.bookfella.booking.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationCreatedEvent(
        String id,
        String userId,
        String hotelId,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal totalPrice
) {}
