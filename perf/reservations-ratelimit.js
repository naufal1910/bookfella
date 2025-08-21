import http from 'k6/http';
import { check, Counter } from 'k6';

export const options = {
  scenarios: {
    rl_probe: {
      executor: 'constant-arrival-rate',
      rate: 400,           // push above Nginx 200 r/s limit
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.8'], // expect many rejections but not >80% failing
    rate_limited: ['count>0'],     // must observe some 429/503
    created_201: ['count>0'],      // must observe some successes
    // Optional perf check for successful writes
    'http_req_duration{status:201}': ['p(95)<350'],
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
  const idem = `${__VU}-${__ITER}-${Date.now()}`; // unique to avoid idempotency conflicts
  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': idem,
  };

  const res = http.post(`${BASE_URL}/api/reservations`, payload, { headers });

  if (res.status === 201) created.add(1);
  if (res.status === 429 || res.status === 503) limited.add(1);

  check(res, {
    'status is 201/429/503': (r) => r && (r.status === 201 || r.status === 429 || r.status === 503),
  });
}
