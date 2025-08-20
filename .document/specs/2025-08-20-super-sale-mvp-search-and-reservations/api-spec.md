openapi: 3.0.3
info:
  title: Super-Sale MVP: Search & Reservations API
  version: 0.1.0
servers:
  - url: http://localhost:8080
    description: App server (Nginx gateway at http://localhost:8081)
paths:
  /api/health:
    get:
      summary: Liveness/Readiness health
      responses:
        '200': { description: OK }
  /actuator/prometheus:
    get:
      summary: Prometheus metrics
      responses:
        '200': { description: Exposes Prometheus metrics }
  /api/search:
    get:
      summary: Search hotels (cache-aside)
      parameters:
        - in: query
          name: q
          schema: { type: string }
        - in: query
          name: city
          schema: { type: string }
        - in: query
          name: page
          schema: { type: integer, minimum: 0, default: 0 }
        - in: query
          name: size
          schema: { type: integer, minimum: 1, maximum: 20, default: 10 }
      responses:
        '200':
          description: List of hotels
          headers:
            Cache-Control:
              schema: { type: string }
              description: Added by Nginx gateway - public, max-age=60
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/Hotel' }
        '400': { $ref: '#/components/responses/BadRequest' }
        '500': { $ref: '#/components/responses/ServerError' }
  /api/reservations:
    post:
      summary: Create reservation (idempotent)
      parameters:
        - in: header
          name: Idempotency-Key
          required: true
          schema: { type: string }
          description: Required; Redis SET NX EX 600 used to enforce idempotency
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/CreateReservationRequest' }
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Reservation' }
        '400': { $ref: '#/components/responses/BadRequest' }
        '409': { $ref: '#/components/responses/Conflict' }
        '500': { $ref: '#/components/responses/ServerError' }
  /api/reservations/{id}:
    get:
      summary: Get reservation by id
      parameters:
        - in: path
          name: id
          required: true
          schema: { type: string }
      responses:
        '200':
          description: Reservation
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Reservation' }
        '404': { $ref: '#/components/responses/NotFound' }
        '500': { $ref: '#/components/responses/ServerError' }
components:
  schemas:
    Hotel:
      type: object
      properties:
        id: { type: string }
        name: { type: string }
        city: { type: string }
        tags:
          type: array
          items: { type: string }
        priceFrom: { type: number, format: float }
    CreateReservationRequest:
      type: object
      required: [ userId, hotelId, checkIn, checkOut, totalPrice ]
      properties:
        userId: { type: string }
        hotelId: { type: string }
        checkIn: { type: string, pattern: "^\\d{4}-\\d{2}-\\d{2}$" }
        checkOut: { type: string, pattern: "^\\d{4}-\\d{2}-\\d{2}$" }
        totalPrice: { type: number, format: double, minimum: 0 }
    Reservation:
      type: object
      properties:
        id: { type: string }
        userId: { type: string }
        hotelId: { type: string }
        checkIn: { type: string }
        checkOut: { type: string }
        totalPrice: { type: number, format: double }
        status: { type: string, enum: [ CREATED ] }
        createdAt: { type: string, format: date-time }
    Problem:
      type: object
      description: RFC7807 problem details
      properties:
        type: { type: string }
        title: { type: string }
        status: { type: integer }
        detail: { type: string }
        instance: { type: string }
  responses:
    BadRequest:
      description: Validation error
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/Problem' }
    Conflict:
      description: Duplicate idempotency key
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/Problem' }
    NotFound:
      description: Not found
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/Problem' }
    ServerError:
      description: Server error
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/Problem' }
