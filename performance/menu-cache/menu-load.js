// 손님 메뉴 조회(목록 LIST_RATIO, 나머지 단건) + 점주 품절 토글. 인기 주점에 몰리게 앞쪽 주점일수록 자주 고른다(u^2 분포)
import http from 'k6/http';
import { check } from 'k6';

const USER = __ENV.USER_URL;
const ADMIN = __ENV.ADMIN_URL;
const STORES = Number(__ENV.STORES || 50);
const LIST_RATIO = Number(__ENV.LIST_RATIO || 1);
const MENUS_PER_STORE = 20;
const DURATION = __ENV.DURATION || '60s';

export const options = {
  discardResponseBodies: true,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
  scenarios: {
    reads: {
      executor: 'constant-arrival-rate', exec: 'read', rate: Number(__ENV.RATE), timeUnit: '1s',
      duration: DURATION, preAllocatedVUs: 200, maxVUs: 2000,
    },
    writes: {
      executor: 'constant-arrival-rate', exec: 'write', rate: Number(__ENV.WRITE_RATE || 1), timeUnit: '1s',
      duration: DURATION, preAllocatedVUs: 2, maxVUs: 10,
    },
  },
  // 손님 조회만 따로 집계되게 하는 형식상 기준 (판정용 아님)
  thresholds: {
    'http_req_duration{scenario:reads}': ['max>=0'],
    'http_reqs{scenario:reads}': ['count>=0'],
    'http_req_failed{scenario:reads}': ['rate<=1'],
  },
};

export function setup() {
  const res = http.post(`${ADMIN}/admin/users/login`,
    JSON.stringify({ email: 'load@test.com', password: 'loadPass1!' }),
    { headers: { 'Content-Type': 'application/json' }, responseType: 'text' });
  return { token: res.json('response.body.accessToken') };
}

const pickStore = () => Math.floor(STORES * Math.random() ** 2) + 1;
const code = (store) => 'LOAD' + String(store).padStart(8, '0');
const menuOf = (store, n) => (store - 1) * MENUS_PER_STORE + n;

export function read() {
  const store = pickStore();
  const res = Math.random() < LIST_RATIO
    ? http.get(`${USER}/v1/stores/${code(store)}/menus`, { tags: { name: 'list' } })
    : http.get(`${USER}/v1/stores/${code(store)}/menus/${menuOf(store, Math.floor(Math.random() * MENUS_PER_STORE) + 1)}`,
      { tags: { name: 'detail' } });
  check(res, { 'read 200': (r) => r.status === 200 });
}

// 각 주점의 1번 메뉴는 반영 지연 측정(probe.sh)용으로 남겨 둔다
export function write(data) {
  const menuId = menuOf(pickStore(), Math.floor(Math.random() * (MENUS_PER_STORE - 1)) + 2);
  const res = http.patch(`${ADMIN}/admin/menus/toggle-soldout/${menuId}`, null,
    { headers: { Authorization: `Bearer ${data.token}` }, tags: { name: 'toggle' } });
  check(res, { 'toggle 200': (r) => r.status === 200 });
}
