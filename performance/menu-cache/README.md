# 메뉴 목록 조회 성능: N+1 → JOIN → 인덱스 → 로컬 캐시 + Redis Pub/Sub 무효화

한 단계씩 바꾸고, 같은 조건에서 직접 잰 것만 적는다. 주제는 3단계(캐시)이고, 1·2단계는 3단계의 AS-IS 를 만든 과정이다.

| 단계 | 주제 | 상태 |
|---|---|---|
| 1 | [N+1 을 JOIN 한 번으로](01-join.md) | 구현·측정 완료 |
| 2 | [menus 에 (store_id, deleted, sort_order) 인덱스](02-index.md) | 구현·측정 완료 |
| 3 | [로컬 캐시 + Redis Pub/Sub 무효화](03-cache.md) | 구현·측정 완료 |

실행 원본: [EC2 측정 (2026-10-26)](results/2026-10-26-ec2.md), [Mac 측정 (2026-10-26)](results/2026-10-26-mac.md)

## 한눈에 (EC2, 2026-10-26)

| 단계 | 요청당 쿼리 | 요청당 읽은 행 | 요청당 MySQL CPU (잰 속도) | p95 200ms 안에서 처리한 최대 부하 | 그때 p95 |
|---|---:|---:|---|---:|---:|
| 기준: N+1 | 22 | 4,041 | 4.7ms (100 req/s) | 100 | 10.0ms |
| 1: JOIN | 2 | 4,042 | 2.3ms (200 req/s) | 500 | 129.5ms |
| 2: + 인덱스 | 2 | 62 | 0.50ms (1,000 req/s) | 2,000 | 57.6ms |
| 3: + 캐시 | 1 | 1 | 0.15ms (1,000 req/s) | 4,000 | 2.2ms |

- 요청당 MySQL CPU 는 MySQL 이 포화되지 않은 속도에서 잰 값이다. 낮은 속도일수록 MySQL 의 기본 사용량이 섞여 조금 크게 나온다
- 3단계에서 남은 쿼리 1개는 주점 조회(`stores`)다. 메뉴 쿼리는 1,000 req/s 60초 동안 59,808 → 123 (99.8% 감소)
- 3단계는 6,000 req/s 에서 user-api CPU 가 3.9/4 코어로 먼저 찼다. 한계가 DB 에서 앱 서버로 옮겨갔다

## 단계별 설정

같은 user-api jar 에서 설정만 바꾼 단계는 차이가 그 설정 하나다.

| 단계 | `run.sh bench` 이름 | user-api jar | menus 인덱스 | 캐시 (`nowait.cache.menu.ttl`) |
|---|---|---|---|---|
| 기준 | `nplus1` | `user-asis.jar` (커밋 `d20c593`, 캐시 작업 전) | 없음 | 캐시 코드 없음 |
| 1 | `join` | `user-tobe.jar` | 없음 | `0s` (끔) |
| 2 | `join-idx` | `user-tobe.jar` | 있음 | `0s` (끔) |
| 3 | `cache` | `user-tobe.jar` | 있음 | `1d` (기본값) |

- admin-api 는 모든 단계에서 `admin.jar`(작업 트리)를 쓴다. 손님 조회 경로에는 들어가지 않고, 품절 토글과 무효화 발행만 한다
- 기준 단계의 user-api 는 조회에 `@Transactional(readOnly = true)` 가 붙어 있다. 1단계부터는 없다(3단계 2절). 그래서 기준 → 1단계 차이에는 트랜잭션 설정 문장도 섞여 있다(1단계 4절)

## 공통 조건

**인스턴스** (쿠폰 발급 토이 프로젝트의 실험용 EC2 세 대를 빌려 썼다)

| 이름 | 유형 | 하는 일 |
|---|---|---|
| db | c7g.large (2 vCPU, 4GiB) | MySQL 8.0.36·Redis 7.2 컨테이너 (각 1 CPU / 1GB 제한). 스키마는 따로 `nowait_lab` |
| app | c7g.xlarge (4 vCPU, 8GiB) | user-api(8081)·admin-api(8085). 둘 다 `-Xms256m -Xmx512m -XX:+UseG1GC` |
| k6 | c7g.large (2 vCPU, 4GiB) | 부하 발생 (k6 v2.1.0) |

서울 리전, 세 대 같은 서브넷, Ubuntu 24.04 (Arm). 앱은 `user-local`·`admin-local` 프로필에 DB·Redis 주소만 바꾸고, SQL 로그를 끄고, Discord 알림 웹훅을 막아 띄운다.

**데이터** (`seed.sql`): 주점 200 × 메뉴 20 = 메뉴 4,000건, 이미지 4,000장. 메뉴마다 이미지 1장인데, 2번 메뉴는 2장, 20번 메뉴는 0장이다(JOIN 결과를 메뉴별로 묶는지, 이미지 없는 메뉴가 빠지지 않는지 보려고).

**트래픽** (`menu-load.js`)
- 손님 메뉴 목록 조회 `GET /v1/stores/{publicCode}/menus` 를 일정한 속도로(constant-arrival-rate)
- 주점은 앞 번호일수록 자주 고른다(`floor(200 × u²) + 1`). 인기 주점에 몰리는 모양
- 동시에 점주 품절 토글 `PATCH /admin/menus/toggle-soldout/{id}` 초당 1건 (각 주점의 1번 메뉴는 반영 지연 측정용으로 빼 둔다)

**순서**: 단계마다 user-api 를 새로 띄우고 품절을 초기화하고 인덱스를 단계에 맞춘다 → 초당 100건 20초 예열(버림) → 속도마다 60초씩 1회.

**측정값**

| 값 | 어디서 |
|---|---|
| 처리 req/s, p50·p95·p99, 실패, 못 보낸 요청 | k6 요약(조회 시나리오만). 처리 req/s 는 끝난 요청 수 ÷ 실행 시간(마무리 대기 포함) |
| 테이블별 쿼리 수·읽은 행·쿼리 시간 | `performance_schema.events_statements_summary_by_digest` (`digest.sql`). 속도마다 비운다. admin 쿼리(토글·로그인, 초당 1건 수준)가 섞인다. 메뉴·이미지 JOIN 쿼리는 `menus` 로 센다 |
| MySQL CPU | MySQL 컨테이너 cgroup `usage_usec` 증가량 ÷ 측정 시간 |
| user-api CPU | user-api JVM 누적 CPU 시간(`ps -o time`) 증가량 ÷ 측정 시간 |
| 히트율 | Micrometer `cache_gets_total{cache="cache:menus"}` 증가량 |

**p95 200ms 안에서 처리한 최대 부하**: 처리 req/s ≥ 목표의 99%, 못 보낸 요청 0, p95 ≤ 200ms 를 모두 만족한 가장 높은 목표 속도. 잰 속도 간격이 넓어(예: 2,000 다음 3,000) 실제 한계는 이보다 높을 수 있다.

## 정합성 확인

`run.sh verify` 가 기존 코드(user `user-asis.jar`, admin `admin-asis.jar`)와 바꾼 코드를 동시에 띄우고, 200개 주점 전부의 메뉴 목록 응답을 문자열로 비교한다.

| 비교 | EC2 | Mac |
|---|---|---|
| user 메뉴 목록 | 같음 200 / 다름 0 | 같음 200 / 다름 0 |
| admin 메뉴 목록 | 같음 200 / 다름 0 | 같음 200 / 다름 0 |

단위 테스트: `MenuRepositoryTest`(H2. JOIN 쿼리 1번, 정렬, 이미지 묶기, 삭제 메뉴·다른 주점 제외, 이미지 없는 메뉴), `DistributedCachedDaoTest`(키 무효화, CLEAR, 재구독 시 비우기).

## 실행 방법

```bash
# jar 네 벌. 기존 코드는 캐시 작업 전 커밋에서 빌드한다
# application-*.yml 은 git 에 없으므로 작업 트리에서 복사하고,
# admin 은 Mac 에서 빌드하면 storePayment 패키지 대소문자 충돌로 뜨지 않아 storepayment 패키지 수정 3파일도 복사한다
git worktree add --detach /tmp/asis d20c593
cp -R nowait-app-user-api/src/main/resources/.  /tmp/asis/nowait-app-user-api/src/main/resources/
cp -R nowait-app-admin-api/src/main/resources/. /tmp/asis/nowait-app-admin-api/src/main/resources/
(cd /tmp/asis && ./gradlew :nowait-app-user-api:bootJar :nowait-app-admin-api:bootJar)   # → user-asis.jar, admin-asis.jar
./gradlew :nowait-app-user-api:bootJar :nowait-app-admin-api:bootJar                      # → user-tobe.jar, admin.jar

# EC2 (쿠폰 프로젝트의 build/ec2/hosts.env 를 읽는다)
JARS=<jar 디렉터리> performance/menu-cache/run.sh setup
performance/menu-cache/run.sh verify
RATES="100 200 400"               performance/menu-cache/run.sh bench nplus1
RATES="200 500 1000"              performance/menu-cache/run.sh bench join
RATES="200 1000 2000 3000 4000"   performance/menu-cache/run.sh bench join-idx
RATES="200 1000 2000 4000 6000"   performance/menu-cache/run.sh bench cache
performance/menu-cache/run.sh delay
performance/menu-cache/run.sh cleanup      # 앱 종료, nowait_lab 삭제

# Mac 한 대 (MySQL·Redis 는 도커, 각 1 CPU / 1GB)
LOCAL=1 JARS=<jar 디렉터리> performance/menu-cache/run.sh setup
LOCAL=1 RATES="200 500 1000" performance/menu-cache/run.sh bench nplus1 join join-idx cache
```

결과는 `performance/menu-cache/out/<단계>/` 에 쌓이고(git 에 올리지 않는다) `report.md` 로 요약된다.

## 공통 한계

- 운영 트래픽이 아니라 실험 환경에서 잰 값이다. 데이터 규모(메뉴 4,000건, 주점당 20개)가 바뀌면 단계별 효과 크기도 바뀐다
- 속도마다 1회만 쟀다(쿠폰 측정은 2회)
- k6 인스턴스 CPU 는 기록하지 않았다. 가장 높은 속도(6,000 req/s)의 "못 보낸 요청" 이 부하 발생기 한계인지 가르지 못한다
- user-api 는 한 대로 쟀다. 여러 대에 무효화가 퍼지는 것은 재지 않았다
