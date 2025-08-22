package com.bookfella.booking.web;

import com.bookfella.booking.dto.CreateReservationRequest;
import com.bookfella.booking.dto.ReservationResponse;
import com.bookfella.booking.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class)
class ReservationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ReservationService service;

    @Test
    void missingIdempotencyHeader_returns400() throws Exception {
        String body = "{\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"hotelId\": \"h1\",\n" +
                "  \"checkIn\": \"2025-09-01\",\n" +
                "  \"checkOut\": \"2025-09-03\",\n" +
                "  \"totalPrice\": 199.99\n" +
                "}";
        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateIdempotency_returns409() throws Exception {
        when(service.create(eq("abc"), any(CreateReservationRequest.class)))
                .thenThrow(new ConflictException("Duplicate request"));

        String body = "{\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"hotelId\": \"h1\",\n" +
                "  \"checkIn\": \"2025-09-01\",\n" +
                "  \"checkOut\": \"2025-09-03\",\n" +
                "  \"totalPrice\": 199.99\n" +
                "}";
        mvc.perform(post("/api/reservations")
                        .header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns201_andBody() throws Exception {
        when(service.create(eq("k1"), any(CreateReservationRequest.class)))
                .thenReturn(new ReservationResponse("r1", "u1", "h1", "2025-09-01", "2025-09-03", new BigDecimal("199.99"), "CREATED", null));

        String body = "{\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"hotelId\": \"h1\",\n" +
                "  \"checkIn\": \"2025-09-01\",\n" +
                "  \"checkOut\": \"2025-09-03\",\n" +
                "  \"totalPrice\": 199.99\n" +
                "}";
        mvc.perform(post("/api/reservations")
                        .header("Idempotency-Key", "k1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("r1"))
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void get_returns200() throws Exception {
        when(service.getById("r1"))
                .thenReturn(new ReservationResponse("r1", "u1", "h1", "2025-09-01", "2025-09-03", new BigDecimal("199.99"), "CREATED", null));

        mvc.perform(get("/api/reservations/r1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("r1"));
    }

    @Test
    void create_invalidDate_returns400() throws Exception {
        String body = "{\n" +
                "  \"userId\": \"u1\",\n" +
                "  \"hotelId\": \"h1\",\n" +
                "  \"checkIn\": \"20250901\",\n" +
                "  \"checkOut\": \"2025-09-03\",\n" +
                "  \"totalPrice\": 199.99\n" +
                "}";
        mvc.perform(post("/api/reservations")
                        .header("Idempotency-Key", "k2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(service.getById("missing")).thenThrow(new NotFoundException("Reservation not found"));
        mvc.perform(get("/api/reservations/missing"))
                .andExpect(status().isNotFound());
    }
}
