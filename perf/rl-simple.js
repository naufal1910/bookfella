import http from 'k6/http';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    rl_probe: {
      executor: 'constant-arrival-rate',
      rate: 300,           // exceed 200 r/s limit but not too high
      timeUnit: '1s',
      duration: '20s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
};

const limited = new Counter('rate_limited');
const created = new Counter('created_201');

export default function () {
  const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';
  const payload = JSON.stringify({
    userId: 'u1',
    hotelId: 'h1',
    checkIn: '2025-09-01',
    checkOut: '2025-09-03',
    totalPrice: 199.99,
  });
  const idem = `${__VU}-${__ITER}-${Date.now()}`;
  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': idem,
  };

  const res = http.post(`${BASE_URL}/api/reservations`, payload, { headers });

  if (res.status === 201) created.add(1);
  if (res.status === 429 || res.status === 503) limited.add(1);
}
