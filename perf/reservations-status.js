import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    reservations_rate: {
      executor: 'constant-arrival-rate',
      rate: 150,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  thresholds: {
    'http_req_duration{status:201}': ['p(95)<350'],
    status_201: ['count>0'],
  },
};

const s201 = new Counter('status_201');
const s409 = new Counter('status_409');
const s429 = new Counter('status_429');
const s503 = new Counter('status_503');
const sOther = new Counter('status_other');

export default function () {
  const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';
  const payload = JSON.stringify({
    userId: 'u1',
    hotelId: 'h1',
    checkIn: '2025-09-01',
    checkOut: '2025-09-03',
    totalPrice: 199.99,
  });
  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `${__VU}-${__ITER}-${Date.now()}`,
  };

  const res = http.post(`${BASE_URL}/api/reservations`, payload, { headers });

  if (res.status === 201) s201.add(1);
  else if (res.status === 409) s409.add(1);
  else if (res.status === 429) s429.add(1);
  else if (res.status === 503) s503.add(1);
  else sOther.add(1);

  check(res, {
    'status acceptable (201/409/429/503)': (r) => r && [201, 409, 429, 503].includes(r.status),
  });
}
