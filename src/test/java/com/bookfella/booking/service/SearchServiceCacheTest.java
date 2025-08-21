package com.bookfella.booking.service;

import com.bookfella.booking.domain.HotelDocument;
import com.bookfella.booking.repo.HotelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchServiceCacheTest {

    private HotelRepository repo;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private SearchService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(HotelRepository.class);
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        objectMapper = new ObjectMapper();
        service = new SearchService(repo, redis, objectMapper);
    }

    @Test
    void cacheMiss_fetchesFromRepo_andSetsCache() throws Exception {
        String key = "search:city:Tokyo";
        when(valueOps.get(key)).thenReturn(null);
        var d1 = new HotelDocument("h1", "Hotel A", "Tokyo", List.of("spa"), new BigDecimal("100.00"));
        when(repo.findByCity("Tokyo")).thenReturn(List.of(d1));

        var result = service.search(null, "Tokyo");

        assertThat(result).hasSize(1);
        verify(repo, times(1)).findByCity("Tokyo");
        verify(valueOps, times(1)).set(eq(key), ArgumentMatchers.anyString());
        verify(redis, times(1)).expire(eq(key), eq(Duration.ofSeconds(60)));
    }

    @Test
    void cacheHit_bypassesRepo() throws Exception {
        String key = "search:q:hotel a"; // normalized
        String payload = "[{\"id\":\"h1\",\"name\":\"Hotel A\",\"city\":\"Tokyo\",\"tags\":[\"spa\"],\"priceFrom\":100.00}]";
        when(valueOps.get(key)).thenReturn(payload);

        var result = service.search("  HOTEL  A  ", null);

        assertThat(result).hasSize(1);
        verify(repo, never()).findByNameContaining(any());
    }
}
