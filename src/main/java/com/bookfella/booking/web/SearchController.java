package com.bookfella.booking.web;

import com.bookfella.booking.dto.HotelDto;
import com.bookfella.booking.service.SearchService;
import jakarta.validation.constraints.Size;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @Timed(value = "api.search")
    public ResponseEntity<List<HotelDto>> search(
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) @Size(max = 100) String city
    ) {
        boolean hasQ = q != null && !q.isBlank();
        boolean hasCity = city != null && !city.isBlank();
        if (!hasQ && !hasCity) {
            throw new BadRequestException("Provide q or city");
        }
        return ResponseEntity.ok(searchService.search(q, city));
    }
}
