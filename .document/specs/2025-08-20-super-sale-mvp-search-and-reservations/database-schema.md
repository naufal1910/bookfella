# Database Schema — RESERVATIONS (Oracle-aligned)

## Oracle DDL
```sql
CREATE TABLE RESERVATIONS (
  ID VARCHAR2(36) PRIMARY KEY,
  USER_ID VARCHAR2(36) NOT NULL,
  HOTEL_ID VARCHAR2(36) NOT NULL,
  CHECK_IN DATE NOT NULL,
  CHECK_OUT DATE NOT NULL,
  TOTAL_PRICE NUMBER(12,2) NOT NULL,
  STATUS VARCHAR2(20) DEFAULT 'CREATED' NOT NULL,
  CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
);

CREATE INDEX IDX_RES_HOTEL ON RESERVATIONS (HOTEL_ID, CHECK_IN, CHECK_OUT);
```

## H2 (Oracle mode) Notes
- Use MODE=Oracle; DATE maps to date, TIMESTAMP supported
- Insert using `TO_DATE('2025-09-01','YYYY-MM-DD')` if needed

## Reservation DTO (reference)
- id: string (UUID)
- userId: string
- hotelId: string
- checkIn/checkOut: YYYY-MM-DD
- totalPrice: decimal(12,2)
- status: CREATED | ...
- createdAt: timestamp
