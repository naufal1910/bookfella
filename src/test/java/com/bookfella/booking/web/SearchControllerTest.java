package com.bookfella.booking.web;

import com.bookfella.booking.dto.HotelDto;
import com.bookfella.booking.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class)
class SearchControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    SearchService searchService;

    @Test
    void returns400_whenMissingBothParams() throws Exception {
        mvc.perform(get("/api/search").accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns200_withCityParam() throws Exception {
        when(searchService.search(eq(null), eq("Tokyo"))).thenReturn(List.of(
                new HotelDto("h1", "Hotel A", "Tokyo", List.of("spa"), new BigDecimal("100.00"))
        ));

        mvc.perform(get("/api/search").param("city", "Tokyo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Tokyo"));
    }
}
