package com.bookfella.booking.service;

import com.bookfella.booking.domain.HotelDocument;
import com.bookfella.booking.dto.HotelDto;
import com.bookfella.booking.repo.HotelRepository;
import com.bookfella.booking.util.KeyUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final TypeReference<List<HotelDto>> LIST_OF_HOTELS = new TypeReference<>() {};

    private final HotelRepository hotelRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SearchService(HotelRepository hotelRepository, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.hotelRepository = Objects.requireNonNull(hotelRepository);
        this.redis = Objects.requireNonNull(redis);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Retry(name = "es")
    @CircuitBreaker(name = "es")
    public List<HotelDto> search(String q, String city) {
        String key;
        boolean byCity = StringUtils.hasText(city);
        if (byCity) {
            key = KeyUtils.searchCityKey(city);
        } else {
            key = KeyUtils.searchQueryKey(q);
        }

        // Try cache
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, LIST_OF_HOTELS);
            } catch (Exception e) {
                // fall through to ES fetch on parse error
            }
        }

        // Fetch from ES
        List<HotelDto> fresh;
        if (byCity) {
            fresh = hotelRepository.findByCity(city).stream().map(SearchService::toDto).collect(Collectors.toList());
        } else {
            String norm = KeyUtils.normalizeQuery(q);
            if (!StringUtils.hasText(norm)) {
                return Collections.emptyList();
            }
            fresh = hotelRepository.findByNameContaining(norm).stream().map(SearchService::toDto).collect(Collectors.toList());
        }

        // Limit page size to 20
        if (fresh.size() > 20) {
            fresh = fresh.subList(0, 20);
        }

        // Cache the result
        try {
            String json = objectMapper.writeValueAsString(fresh);
            // single-call set with TTL to avoid extra RTT
            redis.opsForValue().set(key, json, Duration.ofSeconds(KeyUtils.SEARCH_TTL_SECONDS));
        } catch (Exception ignored) {}

        return fresh;
    }

    private static HotelDto toDto(HotelDocument d) {
        return new HotelDto(d.getId(), d.getName(), d.getCity(), d.getTags(), d.getPriceFrom());
    }
}
