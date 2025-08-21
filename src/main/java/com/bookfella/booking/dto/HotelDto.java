package com.bookfella.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public record HotelDto(
        String id,
        String name,
        String city,
        List<String> tags,
        BigDecimal priceFrom
) {}
