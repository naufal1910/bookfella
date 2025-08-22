package com.bookfella.booking.dto;

import java.math.BigDecimal;

public record ReservationResponse(
        String id,
        String userId,
        String hotelId,
        String checkIn,
        String checkOut,
        BigDecimal totalPrice,
        String status,
        String createdAt
) {}
