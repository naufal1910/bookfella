import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    search_rate: {
      executor: 'constant-arrival-rate',
      rate: 800,
      timeUnit: '1s',
      duration: '50s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<200'],
  },
};

export function setup() {
  const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';
  // Warm cache once before ramp-up
  http.get(`${BASE_URL}/api/search?city=Tokyo`);
}

export default function () {
  const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';
  const res = http.get(`${BASE_URL}/api/search?city=Tokyo`);
  check(res, { 'status is 200': (r) => r.status === 200 });
}
