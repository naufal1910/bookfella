package com.bookfella.booking.web;

import com.bookfella.booking.dto.CreateReservationRequest;
import com.bookfella.booking.dto.ReservationResponse;
import com.bookfella.booking.service.ReservationService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping(path = "/reservations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed(value = "api.reservations.create")
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest req
    ) {
        ReservationResponse created = service.create(idempotencyKey, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping(path = "/reservations/{id}")
    @Timed(value = "api.reservations.get")
    public ResponseEntity<ReservationResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
