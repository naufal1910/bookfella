package com.bookfella.booking.service;

import com.bookfella.booking.domain.HotelDocument;
import com.bookfella.booking.dto.HotelDto;
import com.bookfella.booking.repo.HotelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HotelSearchService {

    private final HotelRepository hotelRepository;

    public HotelSearchService(HotelRepository hotelRepository) {
        this.hotelRepository = Objects.requireNonNull(hotelRepository);
    }

    public List<HotelDto> searchByCity(String city) {
        return hotelRepository.findByCity(city)
                .stream()
                .map(HotelSearchService::toDto)
                .collect(Collectors.toList());
    }

    private static HotelDto toDto(HotelDocument d) {
        return new HotelDto(d.getId(), d.getName(), d.getCity(), d.getTags(), d.getPriceFrom());
    }
}
