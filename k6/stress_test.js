import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        my_heavy_test: {
            executor: 'constant-arrival-rate',
            rate: 200,
            timeUnit: '1s',
            duration: '1m40s',
            preAllocatedVUs: 100,
            maxVUs: 500,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const url = 'http://localhost:8086/api/v1/notifications/test-send';
    const payload = JSON.stringify({
        userId: '550e8400-e29b-41d4-a716-446655440000',
        training_name: 'Extreme Load Workout',
        data: '2026-01-27',
        status: 'ACTIVE'
    });


    const params = {
        headers: { 'Content-Type': 'application/json' },
        timeout: '2s'
    };    const res = http.post(url, payload, params);

    check(res, {
        'status is 202 or 200': (r) => r.status === 202 || r.status === 200,
    });
}