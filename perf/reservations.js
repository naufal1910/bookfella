import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    reservations_rate: {
      executor: 'constant-arrival-rate',
      rate: 150,
      timeUnit: '1s',
      duration: '90s',
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<350'],
  },
};

export default function () {
  const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';
  const payload = JSON.stringify({
    userId: 'u1',
    hotelId: 'h1',
    checkIn: '2025-09-01',
    checkOut: '2025-09-03',
    totalPrice: 199.99
  });
  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `${__VU}-${__ITER}`,
  };
  const res = http.post(`${BASE_URL}/api/reservations`, payload, { headers });
  check(res, { 'status is 201': (r) => r.status === 201 });
}
