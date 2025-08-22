package com.bookfella.booking.service;

import com.bookfella.booking.domain.HotelDocument;
import com.bookfella.booking.dto.HotelDto;
import com.bookfella.booking.repo.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class HotelSearchServiceTest {

    private HotelRepository repo;
    private HotelSearchService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(HotelRepository.class);
        service = new HotelSearchService(repo);
    }

    @Test
    void searchByCity_maps_documents_to_dto() {
        var d1 = new HotelDocument("h1", "Hotel A", "Tokyo", List.of("spa"), new BigDecimal("120.50"));
        var d2 = new HotelDocument("h2", "Hotel B", "Tokyo", List.of("onsen"), new BigDecimal("80.00"));
        when(repo.findByCity("Tokyo")).thenReturn(List.of(d1, d2));

        List<HotelDto> result = service.searchByCity("Tokyo");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Hotel A");
        assertThat(result.get(1).priceFrom()).isEqualByComparingTo("80.00");
    }
}
