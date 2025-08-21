package com.bookfella.booking.config;

import com.bookfella.booking.domain.HotelDocument;
import com.bookfella.booking.repo.HotelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("!test")
public class ElasticsearchIndexConfig implements CommandLineRunner {

    private final ElasticsearchOperations operations;
    private final HotelRepository hotelRepository;

    public ElasticsearchIndexConfig(ElasticsearchOperations operations, HotelRepository hotelRepository) {
        this.operations = operations;
        this.hotelRepository = hotelRepository;
    }

    @Override
    public void run(String... args) {
        IndexOperations indexOps = operations.indexOps(HotelDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            // mapping based on annotations
            Document mapping = indexOps.createMapping(HotelDocument.class);
            indexOps.putMapping(mapping);
        }

        if (hotelRepository.count() == 0) {
            hotelRepository.saveAll(List.of(
                    new HotelDocument("h1", "Hotel A", "Tokyo", List.of("spa"), new BigDecimal("120.50")),
                    new HotelDocument("h2", "Hotel B", "Tokyo", List.of("onsen"), new BigDecimal("80.00")),
                    new HotelDocument("h3", "Hotel C", "Osaka", List.of("city"), new BigDecimal("99.99"))
            ));
        }
    }
}
