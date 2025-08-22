package com.bookfella.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateReservationRequest(
        @NotBlank String userId,
        @NotBlank String hotelId,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "checkIn must be yyyy-MM-dd") String checkIn,
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "checkOut must be yyyy-MM-dd") String checkOut,
        @Positive BigDecimal totalPrice
) {}
