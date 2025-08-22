package com.bookfella.booking.repo;

import com.bookfella.booking.domain.Reservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryTest {

    @Autowired
    ReservationRepository repository;

    @Test
    void save_and_fetch_reservation() {
        Reservation r = new Reservation();
        r.setId("res-1");
        r.setUserId("u1");
        r.setHotelId("h1");
        r.setCheckIn(LocalDate.parse("2025-09-01"));
        r.setCheckOut(LocalDate.parse("2025-09-03"));
        r.setTotalPrice(new BigDecimal("199.99"));

        repository.save(r);

        Optional<Reservation> found = repository.findById("res-1");
        assertThat(found).isPresent();
        assertThat(found.get().getHotelId()).isEqualTo("h1");
        assertThat(found.get().getTotalPrice()).isEqualByComparingTo("199.99");
    }
}
