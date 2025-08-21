package com.bookfella.booking.repo;

import com.bookfella.booking.domain.HotelDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface HotelRepository extends ElasticsearchRepository<HotelDocument, String> {
    List<HotelDocument> findByCity(String city);
}
