import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 50 },
    { duration: '10s', target: 50 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<200'],
  },
};

const BASE_URL = __ENV.GW_URL || 'http://localhost:8081';

export function setup() {
  // warm cache
  http.get(`${BASE_URL}/api/search?city=Tokyo`);
}

export default function () {
  const res = http.get(`${BASE_URL}/api/search?city=Tokyo`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.1);
}
