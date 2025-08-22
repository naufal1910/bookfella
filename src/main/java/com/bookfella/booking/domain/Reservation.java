package com.bookfella.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESERVATIONS")
public class Reservation {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "USER_ID", length = 36, nullable = false)
    private String userId;

    @Column(name = "HOTEL_ID", length = 36, nullable = false)
    private String hotelId;

    @Column(name = "CHECK_IN", nullable = false)
    private LocalDate checkIn;

    @Column(name = "CHECK_OUT", nullable = false)
    private LocalDate checkOut;

    @Column(name = "TOTAL_PRICE", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    // Persist status from application (default to 'CREATED' at service layer)
    @Column(name = "STATUS", nullable = false)
    private String status;

    // Use DB default SYSTIMESTAMP
    @Column(name = "CREATED_AT", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Reservation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
